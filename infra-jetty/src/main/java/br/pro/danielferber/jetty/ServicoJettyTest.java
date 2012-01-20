package br.pro.danielferber.jetty;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.jetty.embedded.HelloHandler;
import org.eclipse.jetty.embedded.HelloServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;

import br.pro.danielferber.infra.exception.ServicoExcecao;
import br.pro.danielferber.infra.exception.motivo.MotivoException;

public class ServicoJettyTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
//			
//			sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", RESOURCE_CONFIG);
//			sh.setInitParameter("com.sun.jersey.config.property.packages", handlerPackageLocation);
//			Context context = new Context(server, "/", Context.SESSIONS);
			ServicoJetty.getInstance().start(new Runnable() {
				@Override
				public void run() {
					Server server = ServicoJetty.getInstance().getServer();
					ServletHolder sh = new ServletHolder(ServletContainer.class);
					sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", PackagesResourceConfig.class.getName());
					sh.setInitParameter("com.sun.jersey.config.property.packages", "br.pro.danielferber.jetty");					
					ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
					context.addServlet(sh, "/*");
				}
			});
			ServicoJetty.getInstance().stop();
		} catch (MotivoException e) {
			ServicoExcecao.reportarException(System.out, e);
		}
	}
	
    @Path("/")
    public static class TestResource {

        @GET
        public String get() {
            return "GET";
        }
    }
}
