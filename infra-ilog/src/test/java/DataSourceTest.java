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
import infra.exception.ServicoExcecao;
import infra.exception.motivo.MotivoException;
import infra.ilog.cplex.ConfiguracaoCplex;
import infra.ilog.opl.ConfiguracaoOPL;
import infra.ilog.opl.FacadeOPL;
import infra.ilog.opl.FonteDados;
import infra.ilog.opl.ProvedorModelo;
import infra.ilog.opl.dados.FonteDadosArquivo;
import infra.ilog.opl.dados.FonteDadosClasspath;
import infra.ilog.opl.dados.FonteDadosInputStream;
import infra.ilog.opl.dados.FonteDadosString;
import infra.ilog.opl.modelo.ProvedorModeloString;

import java.io.File;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;



@SuppressWarnings("deprecation")
public class DataSourceTest {
	private ConfiguracaoOPL configuracaoOpl;
	private ConfiguracaoCplex configuracaoCplex;
	private ProvedorModelo provedorModelo;

	@Before
	public void loadModel() {
		File homeDir = new File(System.getProperty("user.dir"));
		configuracaoOpl = new ConfiguracaoOPL("teste", homeDir);
		configuracaoCplex = new ConfiguracaoCplex("teste", homeDir);
		String modeloString =
				"using CPLEX;" +
						"" +
						"float a = ...;" +
						"float b = ...;" +
						"" +
						"dvar float x;" +
						"dvar float y;" +
						"" +
						"maximize a*x + b*y;" +
						"" +
						"subject to {" +
						"	x <= 5;" +
						"	y <= 5;" +
						"	y <= 7 - x;" +
						"}";
		provedorModelo = new ProvedorModeloString("modelo", modeloString);
	}

	@Test
	public void provedorStringTest() {
		String dadosString = "a = 3; b = 2;";

		FonteDados provedorDados = new FonteDadosString("nome", dadosString);
		Collection<FonteDados> provedoresDados = new HashSet<FonteDados>();
		provedoresDados.add(provedorDados);
		FacadeOPL facadeOPL = new FacadeOPL(configuracaoOpl, configuracaoCplex, provedorModelo, provedoresDados, null);
		try {
			facadeOPL.executar();
		} catch (MotivoException e) {
			ServicoExcecao.reportarException(System.err, e);
			Assert.fail(e.getMessage());
		} finally {
			facadeOPL.dispose();
		}
	}

	@Test
	public void provedorInputStreamTest() {
		String dadosString = "a = 3; b = 2;";
		InputStream is = new StringBufferInputStream(dadosString);
		FonteDados provedorDados = new FonteDadosInputStream("nome", is );
		Collection<FonteDados> provedoresDados = new HashSet<FonteDados>();
		provedoresDados.add(provedorDados);
		FacadeOPL facadeOPL = new FacadeOPL(configuracaoOpl, configuracaoCplex, provedorModelo, provedoresDados, null);
		try {
			facadeOPL.executar();
		} catch (MotivoException e) {
			ServicoExcecao.reportarException(System.err, e);
			Assert.fail(e.getMessage());
		} finally {
			facadeOPL.dispose();
		}
	}

	@Test
	public void provedorArquivoTest() {
		FonteDados provedorDados = new FonteDadosArquivo("nome", new File("src/test/resources/dados/poliedro.dat") );
		Collection<FonteDados> provedoresDados = new HashSet<FonteDados>();
		provedoresDados.add(provedorDados);
		FacadeOPL facadeOPL = new FacadeOPL(configuracaoOpl, configuracaoCplex, provedorModelo, provedoresDados, null);
		try {
			facadeOPL.executar();
		} catch (MotivoException e) {
			ServicoExcecao.reportarException(System.err, e);
			Assert.fail(e.getMessage());
		} finally {
			facadeOPL.dispose();
		}
	}

	@Test
	public void provedorClasspathTest() {
		FonteDados provedorDados = new FonteDadosClasspath("nome", "/dados/poliedro.dat");
		Collection<FonteDados> provedoresDados = new HashSet<FonteDados>();
		provedoresDados.add(provedorDados);
		FacadeOPL facadeOPL = new FacadeOPL(configuracaoOpl, configuracaoCplex, provedorModelo, provedoresDados, null);
		try {
			facadeOPL.executar();
		} catch (MotivoException e) {
			ServicoExcecao.reportarException(System.err, e);
			Assert.fail(e.getMessage());
		} finally {
			facadeOPL.dispose();
		}
	}
}
