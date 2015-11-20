/*
 *
 *  * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package thredds;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.junit.Assert;
import thredds.util.ContentType;
import ucar.httpservices.*;
import ucar.nc2.util.IO;

import java.io.File;
import java.io.InputStream;

/**
 * Describe
 *
 * @author caron
 * @since 10/15/13
 */
public class TestWithLocalServer {
  public static String server = "http://localhost:8081/thredds/";

  public static String withPath(String path) {
    return server + StringUtils.stripStart(path, "/\\");  // Remove leading slashes from path.
  }

  public static byte[] getContent(String endpoint, int expectCode, ContentType expectContentType) {
    System.out.printf("req = '%s'%n", endpoint);
    try (HTTPSession session = new HTTPSession(endpoint)) {
      HTTPMethod method = HTTPFactory.Get(session);
      int statusCode = method.execute();
      if (statusCode != 200) {
        System.out.printf("statusCode = %d '%s'%n", statusCode, method.getResponseAsString());
        Assert.assertEquals(expectCode, statusCode);
        return null;
      }

      Assert.assertEquals(expectCode, statusCode);

      if (expectContentType != null) {
        Header header = method.getResponseHeader(ContentType.HEADER);
        Assert.assertEquals(expectContentType.getContentHeader(), header.getValue());
      }

      byte[] content = method.getResponseAsBytes();
      // assert content.length > 0;
      return content;

    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }

    return null;
  }

  public static void saveContentToFile(String endpoint, int expectCode, ContentType expectContentType, File saveTo) {
    System.out.printf("req = '%s'%n", endpoint);
    try (HTTPSession session = new HTTPSession(endpoint)) {
      HTTPMethod method = HTTPFactory.Get(session);
      int statusCode = method.execute();
      if (statusCode != 200) {
        System.out.printf("statusCode = %d '%s'%n", statusCode, method.getResponseAsString());
        Assert.assertEquals(expectCode, statusCode);
        return;
      }

      Assert.assertEquals(expectCode, statusCode);

      if (expectContentType != null) {
        Header header = method.getResponseHeader(ContentType.HEADER);
        Assert.assertEquals(expectContentType.getContentHeader(), header.getValue());
      }

      InputStream content = method.getResponseAsStream(); // closing method may close stream ??
      IO.appendToFile(content, saveTo.getAbsolutePath());

    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }
  }


  public static String testWithHttpGet(CredentialsProvider provider, String endpoint, ContentType expectContentType) {
    System.out.printf("testOpenXml req = '%s'%n", endpoint);

    try (HTTPSession session = new HTTPSession(endpoint)) {
      session.setCredentialsProvider(endpoint, provider);
      HTTPMethod method = HTTPFactory.Get(session);
      int statusCode = method.execute();
      Assert.assertEquals(200, statusCode);

      String response = method.getResponseAsString();
      assert response.length() > 0;

      Header header = method.getResponseHeader(ContentType.HEADER);
      Assert.assertEquals(expectContentType.getContentHeader(), header.getValue());

      return response;

    } catch (HTTPException e) {
      e.printStackTrace();
      assert false;
    }

    return null;
  }

  public static String testWithHttpGet(Credentials cred, String endpoint, int expectedStatus, ContentType expectContentType) {
    System.out.printf("testOpenXml req = '%s'%n", endpoint);

    int pos = endpoint.indexOf("?");
    String url = pos < 0 ? endpoint : endpoint.substring(0, pos);

    try (HTTPSession session = new HTTPSession(endpoint)) {
      session.setCredentials(url, cred);
      HTTPMethod method = HTTPFactory.Get(session);
      int statusCode = method.execute();
      Assert.assertEquals(expectedStatus, statusCode);
      if (expectedStatus != 200) return "ok";

      String response = method.getResponseAsString();
      assert response.length() > 0;

      Header header = method.getResponseHeader(ContentType.HEADER);
      Assert.assertEquals(expectContentType.getContentHeader(), header.getValue());

      return response;

    } catch (HTTPException e) {
      e.printStackTrace();
      assert false;
    }

    return null;
  }

}
