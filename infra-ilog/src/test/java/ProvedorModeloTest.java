/*
 * Copyright 2012 Daniel Felix Ferber
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import ilog.cplex.ConfiguracaoCplex;
import ilog.opl.ConfiguracaoOPL;
import ilog.opl.FacadeOPL;
import ilog.opl.ProvedorModelo;
import ilog.opl.modelo.ProvedorModeloArquivo;
import ilog.opl.modelo.ProvedorModeloClasspath;
import ilog.opl.modelo.ProvedorModeloString;
import infra.exception.ServicoExcecao;
import infra.exception.motivo.MotivoException;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;



public class ProvedorModeloTest {
	private ConfiguracaoOPL configuracaoOpl;
	private ConfiguracaoCplex configuracaoCplex;

	@Before
	public void loadModel() {
		File homeDir = new File(System.getProperty("user.dir"));
		configuracaoOpl = new ConfiguracaoOPL(homeDir);
		configuracaoCplex = new ConfiguracaoCplex(homeDir);
	}

	@Test
	public void provedorStringTest() {
		String modeloString =
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
			"  x >= 0;" +
			"  y >= 0;" +
			"}";

		ProvedorModelo provedorModelo = new ProvedorModeloString("nome", modeloString);

		FacadeOPL facadeOPL = new FacadeOPL(configuracaoOpl, configuracaoCplex, provedorModelo, null, null);
		try {
			facadeOPL.executar(configuracaoCplex);
		} catch (MotivoException e) {
			ServicoExcecao.reportarException(System.err, e);
			Assert.fail(e.getMessage());
		} finally {
			facadeOPL.dispose();
		}
	}

	@Test
	public void provedorArquivoTest() {
		ProvedorModelo provedorModelo = new ProvedorModeloArquivo("nome", new File("src/test/resources/modelos/pontos.mod"));

		FacadeOPL facadeOPL = new FacadeOPL(configuracaoOpl, configuracaoCplex, provedorModelo, null, null);
		try {
			facadeOPL.executar(configuracaoCplex);
		} catch (MotivoException e) {
			ServicoExcecao.reportarException(System.err, e);
			Assert.fail(e.getMessage());
		} finally {
			facadeOPL.dispose();
		}
	}

	@Test
	public void provedorClasspathTest() {
		ProvedorModelo provedorModelo = new ProvedorModeloClasspath("nome", "/modelos/pontos.mod");

		FacadeOPL facadeOPL = new FacadeOPL(configuracaoOpl, configuracaoCplex, provedorModelo, null, null);
		try {
			facadeOPL.executar(configuracaoCplex);
		} catch (MotivoException e) {
			ServicoExcecao.reportarException(System.err, e);
			Assert.fail(e.getMessage());
		} finally {
			facadeOPL.dispose();
		}
	}

}
