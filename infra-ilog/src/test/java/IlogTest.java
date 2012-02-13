import ilog.cplex.IloCplex;
import ilog.opl.IloOplErrorHandler;
import ilog.opl.IloOplFactory;
import ilog.opl.IloOplModel;
import ilog.opl.IloOplModelDefinition;
import ilog.opl.IloOplModelSource;
import ilog.opl.IloOplSettings;

import org.junit.Test;


public class IlogTest {
	@Test
	public void runIlogTest() throws Exception {
		System.out.println(System.getProperty("java.library.path"));
		System.out.println(System.getProperty("library.path"));

		IloOplFactory oplF = new IloOplFactory();
		IloOplErrorHandler errHandler = oplF.createOplErrorHandler();
		IloOplSettings settings = oplF.createOplSettings(errHandler);
	    IloOplModelSource modelSource = oplF.createOplModelSource("src/test/resources/modelos/pontos.mod");
	    IloOplModelDefinition def = oplF.createOplModelDefinition(modelSource, settings);
	    IloCplex cplex = oplF.createCplex();
	    IloOplModel opl = oplF.createOplModel(def, cplex);
	    opl.generate();
	    cplex.solve();
	}
}
