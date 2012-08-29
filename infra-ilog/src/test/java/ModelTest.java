import infra.exception.ExceptionService;
import infra.ilog.NoSolutionException;
import infra.ilog.cplex.ConfigurationCplex;
import infra.ilog.opl.ConfiguracaoOPL;
import infra.ilog.opl.FacadeOPL;
import infra.ilog.opl.OplModelException;
import infra.ilog.opl.ProvedorModelo;
import infra.ilog.opl.modelo.ProvedorModeloString;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * Verifica se um modelo incorreto é acusado de forma adequada.
 */
public class ModelTest {
	private ConfiguracaoOPL configuracaoOpl;
	private ConfigurationCplex configuracaoCplex;

	@Before
	public void loadConfiguration() {
		File homeDir = new File(System.getProperty("user.dir"));
		configuracaoOpl = new ConfiguracaoOPL("teste", homeDir);
		configuracaoCplex = new ConfigurationCplex("teste", homeDir);
	}

	@Test
	public void modeloInvalido1Test() {
		String modeloInvalido1 =
				"tuple ponto {" +
						"	float x; " +
						"	float y;" +
						"}" +
						"range espaco = 1..2" + // aqui faltou terminar com ponto-e-virgula.
						"ponto pontos[espaco] = [ <0,3>, <3,0> ];" +
						"ponto referencia = <1,1>;" +
						"dvar float pesos[espaco];" +
						"dexpr float x = sum(i in espaco) pesos[i] * pontos[i].x;" +
						"dexpr float y = sum(i in espaco) pesos[i] * pontos[i].y;" +
						"minimize (x - referencia.x)^2 + (y - referencia.y)^2;" +
						"subject to {" +
						"  sum(i in espaco) pesos[i] == 1.0;" +
						"  x >= 0;" +
						"  y >= 0;" +
						"}";

		ProvedorModelo provedorModelo1 = new ProvedorModeloString("nome", modeloInvalido1);

		FacadeOPL facadeOPL = new FacadeOPL(configuracaoOpl, configuracaoCplex, provedorModelo1, null, null);
		try {
			facadeOPL.executar();
			Assert.fail("Model should be accused as invalid.");
		} catch (OplModelException e) {
			ExceptionService.reportarException(System.err, e);
		} catch (NoSolutionException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void modeloInvalido2Test() {
		String modeloInvalido1 =
				"tuple ponto {" +
						"	float x; " +
						"	float y;" +
						"}" +
						"range espaco = 1..2;" +
						"ponto pontos[espaco] = [ <0,3>, <3,0> ];" +
						"ponto referencia = <1,1>;" +
						"dvar float pesos[espaco];" +
						"dexpr float x = sum(i in espaco) pesos[i] * pontos[i].x;" +
						"dexpr float y = sum(i in espaco) pesos[i] * pontos[i].y;" +
						"minimize (x - referencia.x)^2 + (y - referencia.y)^2;" +
						"subject to {" +
						"  sum(i in espaco) pesos[i] == 1.0;" +
						"  wrubbles >= 0;" + // aqui tem uma variável nada a ver
						"  y >= 0;" +
						"}";

		ProvedorModelo provedorModelo1 = new ProvedorModeloString("nome", modeloInvalido1);

		FacadeOPL facadeOPL = new FacadeOPL(configuracaoOpl, configuracaoCplex, provedorModelo1, null, null);
		try {
			facadeOPL.executar();
			Assert.fail("Model should be accused as invalid.");
		} catch (OplModelException e) {
			ExceptionService.reportarException(System.err, e);
		} catch (NoSolutionException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void modeloInvalido3Test() {
		String modeloInvalido1 =
				"tuple ponto {" +
						"	float x; " +
						"	float y;" +
						"}" +
						"range espaco = 1..2;" +
						"ponto pontos[espaco] = [ <0,3>, <3,0>, <4,0> ];" + // numero de elementos não é compatível com definição do índice
						"ponto referencia = <1,1>;" +
						"dvar float pesos[espaco];" +
						"dexpr float x = sum(i in espaco) pesos[i] * pontos[i].x;" +
						"dexpr float y = sum(i in espaco) pesos[i] * pontos[i].y;" +
						"minimize (x - referencia.x)^2 + (y - referencia.y)^2;" +
						"subject to {" +
						"  sum(i in espaco) pesos[i] == 1.0;" +
						"  x >= 0;" +
						"  y >= 0;" +
						"}";

		ProvedorModelo provedorModelo1 = new ProvedorModeloString("nome", modeloInvalido1);

		FacadeOPL facadeOPL = new FacadeOPL(configuracaoOpl, configuracaoCplex, provedorModelo1, null, null);
		try {
			facadeOPL.executar();
			Assert.fail("Model should be accused as invalid.");
		} catch (OplModelException e) {
			ExceptionService.reportarException(System.err, e);
		} catch (NoSolutionException e) {
			e.printStackTrace();
		}
	}
}
