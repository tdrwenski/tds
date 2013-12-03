package ucar.nc2.grib.collection;

import net.jcip.annotations.Immutable;
import ucar.arr.Coordinate;
import ucar.arr.CoordinateBuilder;
import ucar.arr.CoordinateBuilderImpl;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Indent;

import java.util.*;

/**
 * Time coordinates that are not intervals.
 *
 * @author caron
 * @since 11/24/13
 */
@Immutable
public class CoordinateTime implements Coordinate, Comparable<CoordinateTime> {
  private final CalendarDate runtime;
  private final List<Integer> offsetSorted;
  private final int code;

  public CoordinateTime(CalendarDate runtime, List<Integer> offsetSorted, int code) {
    this.runtime = runtime;
    this.offsetSorted = Collections.unmodifiableList(offsetSorted);
    this.code = code;
  }

  static public Integer extractOffset(Grib2Record gr) {
    Grib2Pds pds = gr.getPDS();
    return pds.getForecastTime();
  }

  public Integer extract(Grib2Record gr) {
    return extractOffset(gr);
  }

  public int compareTo(CoordinateTime o) {
    return runtime.compareTo(o.runtime);
  }

  public CalendarDate getRuntime() {
    return runtime;
  }

  public List<Integer> getOffsetSorted() {
    return offsetSorted;
  }

  @Override
  public List<? extends Object> getValues() {
    return offsetSorted;
  }

  @Override
  public int getSize() {
    return offsetSorted.size();
  }

  public Type getType() {
    return Type.time;
  }

  @Override
  public String getUnit() {
    return null;
  }

  public int getCode() {
    return code;
  }
  /* public List<Grib2Record> getRecordList(int timeIdx) {
    return recordList.get(timeIdx);
  } */

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s %20s:", indent, "Offsets");
     for (Integer cd : offsetSorted)
       info.format("%3d, ", cd);
    info.format("%n");
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Time offsets:%n");
     for (Integer cd : offsetSorted)
       info.format("   %3d%n", cd);
  }

  static public class Builder extends CoordinateBuilderImpl  {
    private final CalendarDate runtime;
    int code;

    public Builder(Object runtime, int code) {
      super(runtime);
      this.runtime = (CalendarDate) runtime;
      this.code = code;
    }

    @Override
    public CoordinateBuilder makeBuilder(Object val) {
      CoordinateBuilder result =  new Builder(val, code);
      result.chainTo(nestedBuilder);
      return result;
    }

    @Override
    protected Object extract(Grib2Record gr) {
      return extractOffset(gr);
    }

    @Override
   protected Coordinate makeCoordinate(List<Object> values, List<Coordinate> subdivide) {
      List<Integer> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) offsetSorted.add( (Integer) val);
      Collections.sort(offsetSorted);
      return new CoordinateTime(runtime, offsetSorted, code);
    }
  }

}