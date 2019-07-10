package eu.arrowhead.client.common.exception;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ContainerRequest;

@Provider
public class NotSupportedMediaTypeExceptionMapper implements ExceptionMapper<NotSupportedException> {

  @Inject
  private javax.inject.Provider<ContainerRequest> requestContext;

  @Override
  public Response toResponse(NotSupportedException ex) {
    ex.printStackTrace();
    ErrorMessage errorMessage;
    if (ex.getMessage() != null) {
      errorMessage = new ErrorMessage(ex.getMessage(), 405, ExceptionType.BAD_MEDIA_TYPE,
                                      requestContext.get().getBaseUri().toString());
    } else {
      List<String> requestContentType = requestContext.get().getHeaders().get("Content-Type");
      errorMessage = new ErrorMessage(
          requestContentType.toString() + " is not allowed at " + requestContext.get().getPath(true), 405,
          ExceptionType.BAD_MEDIA_TYPE, requestContext.get().getBaseUri().toString());
    }

    return Response.status(Status.UNSUPPORTED_MEDIA_TYPE).entity(errorMessage)
                   .header("Content-type", "application/json")
                   .build();
  }
}
