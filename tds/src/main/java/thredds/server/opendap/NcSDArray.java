/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.opendap;

import opendap.dap.InvalidDimensionException;
import ucar.ma2.*;
import ucar.nc2.*;
import opendap.servers.*;
import opendap.dap.BaseType;
import opendap.dap.DArrayDimension;
import opendap.dap.PrimitiveVector;
import java.io.IOException;
import java.io.EOFException;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * {@literal Wraps a netcdf variable with rank > 0 as an SDArray.
 * For char arrays, use NcSDString (rank 0 or 1) or NcSDCharArray (rank > 1).}
 * 
 * @author jcaron
 * @see NcSDCharArray
 */
public class NcSDArray extends SDArray implements HasNetcdfVariable {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcSDArray.class);

  private static final boolean debug = false, debugRead = false;
  private Variable ncVar = null;
  // ignore protected BaseType elemType;

  /**
   * Constructor: Wraps a netcdf variable in a DODS SDArray.
   *
   * @param v : netcdf Variable
   * @param bt : DODS element type
   */
  NcSDArray(Variable v, BaseType bt) {
    super(Variable.getDAPName(v));
    this.ncVar = v;

    // set dimensions
    for (Dimension dim : v.getDimensions()) {
      appendDim(dim.getLength(), dim.getShortName());
    }

    // this seems to be how you set the type
    // it creates the "primitive vector"
    addVariable(bt);
    // ignore this.elemType = bt;
  }

  public Variable getVariable() {
    return ncVar;
  }

  /**
   * Read the data values (parameters are ignored).
   * Use the start, stop and stride values, typically set by the constraint evaluator.
   *
   * @param datasetName not used
   * @param specialO not used
   * @return false (no more data to be read)
   * @throws IOException
   * @throws EOFException
   */
  public boolean read(String datasetName, Object specialO) throws IOException {
    long tstart = System.currentTimeMillis();

    Array a;
    try {
      if (log.isDebugEnabled())
        log.debug(getRequestedRange());

      // set up the netcdf read
      int n = numDimensions();
      List<Range> ranges = new ArrayList<>(n);
      for (int i = 0; i < n; i++)
        ranges.add(new Range(getStart(i), getStop(i), getStride(i)));

      try {
        a = ncVar.read(ranges);

      } catch (java.lang.ArrayIndexOutOfBoundsException t) {
        log.error(getRequestedRange(), t);
        throw new RuntimeException("NcSDArray java.lang.ArrayIndexOutOfBoundsException=" + t.getMessage()
            + " for request= " + getRequestedRange() + " dataset= " + datasetName, t);
      }

      if (debug)
        System.out.println(
            "  NcSDArray Read " + getEncodedName() + " " + a.getSize() + " elems of type = " + a.getElementType());
      if (debugRead)
        System.out.println("  Read = " + a.getSize() + " elems of type = " + a.getElementType());
      if (log.isDebugEnabled()) {
        long tookTime = System.currentTimeMillis() - tstart;
        log.debug("NcSDArray read array: " + tookTime * .001 + " seconds");
      }

    } catch (InvalidDimensionException e) {
      log.error(getRequestedRange(), e);
      throw new IllegalStateException("NcSDArray InvalidDimensionException=" + e.getMessage());

    } catch (InvalidRangeException e) {
      log.error(getRequestedRange(), e);
      throw new IllegalStateException("NcSDArray InvalidRangeException=" + e.getMessage());
    }
    setData(a);

    if (debugRead)
      System.out.println(" PrimitiveVector len = " + getPrimitiveVector().getLength() + " type = "
          + getPrimitiveVector().getTemplate());

    return (false);
  }

  private String getRequestedRange() {
    try {
      StringBuilder sbuff = new StringBuilder();
      sbuff.append("NcSDArray read " + ncVar.getFullName());
      for (int i = 0; i < numDimensions(); i++) {
        DArrayDimension d = getDimension(i);
        sbuff.append(" " + d.getEncodedName() + "(" + getStart(i) + "," + getStride(i) + "," + getStop(i) + ")");
      }
      return sbuff.toString();

    } catch (InvalidDimensionException e) {
      e.printStackTrace();
      return e.getMessage();
    }
  }

  public void setData(Array data) {
    PrimitiveVector pv = getPrimitiveVector();
    if (debugRead)
      System.out.println(" PrimitiveVector type = " + pv.getTemplate() + " pv type = " + pv.getClass().getName());

    if (ncVar.getDataType() == DataType.STRING) {
      int size = (int) data.getSize();
      NcSDString[] dodsBT = new NcSDString[size];
      IndexIterator ii = data.getIndexIterator();
      int count = 0;
      while (ii.hasNext()) {
        dodsBT[count++] = new NcSDString(Variable.getDAPName(ncVar), (String) ii.getObjectNext());
      }
      pv.setInternalStorage(dodsBT);

    } else if (ncVar.getDataType() == DataType.STRUCTURE) {
      NcSDStructure sds = (NcSDStructure) pv.getTemplate();

      int size = (int) data.getSize();
      NcSDStructure[] dodsBT = new NcSDStructure[size];

      IndexIterator ii = data.getIndexIterator();
      int count = 0;
      while (ii.hasNext()) {
        StructureData sdata = (StructureData) ii.getObjectNext();
        dodsBT[count] = new NcSDStructure(sds, sdata); // stupid replication - need to override externalize
        count++;
      }
      pv.setInternalStorage(dodsBT);

    } else {

      // copy the data into the PrimitiveVector
      // this is optimized to (possibly) eliminate the copy
      Object pa = data.get1DJavaArray(data.getElementType());
      pv.setInternalStorage(pa);
    }

    setRead(true);
  }

  public void serialize(DataOutputStream sink, StructureData sdata, StructureMembers.Member m) throws IOException {
    long tstart = System.currentTimeMillis();

    setData(sdata.getArray(m));
    externalize(sink);

    if (log.isDebugEnabled()) {
      long tookTime = System.currentTimeMillis() - tstart;
      log.debug("NcSDArray serialize: " + tookTime * .001 + " seconds");
    }
  }

}
