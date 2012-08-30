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
import infra.exception.ExceptionService;
import infra.ilog.cplex.ConfigurationCplex;
import infra.ilog.opl.ConfiguracaoOPL;
import infra.ilog.opl.FacadeOPL;
import infra.ilog.opl.DataSource;
import infra.ilog.opl.ProvedorModelo;
import infra.ilog.opl.dados.DataSourceFile;
import infra.ilog.opl.dados.DataSourceClasspath;
import infra.ilog.opl.dados.DataSourceInputStream;
import infra.ilog.opl.dados.DataSourceString;
import infra.ilog.opl.modelo.ProvedorModeloString;

import java.io.File;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifica se os possíveis datasources estão funcionando corretamente.
 */
@SuppressWarnings("deprecation")
public class DataSourceTest {
	private ConfiguracaoOPL configuracaoOpl;
	private ConfigurationCplex configuracaoCplex;
	private ProvedorModelo provedorModelo;

	@Before
	public void loadModel() {
		File homeDir = new File(System.getProperty("user.dir"));
		configuracaoOpl = new ConfiguracaoOPL("teste", homeDir);
		configuracaoCplex = new ConfigurationCplex("teste", homeDir);
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

		DataSource provedorDados = new DataSourceString("nome", dadosString);
		Collection<DataSource> provedoresDados = new HashSet<DataSource>();
		provedoresDados.add(provedorDados);
		FacadeOPL facadeOPL = new FacadeOPL(configuracaoOpl, configuracaoCplex, provedorModelo, provedoresDados, null);
		try {
			facadeOPL.executar();
		} catch (Exception e) {
			ExceptionService.reportException(System.err, e);
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void provedorInputStreamTest() {
		String dadosString = "a = 3; b = 2;";
		InputStream is = new StringBufferInputStream(dadosString);
		DataSource provedorDados = new DataSourceInputStream("nome", is );
		Collection<DataSource> provedoresDados = new HashSet<DataSource>();
		provedoresDados.add(provedorDados);
		FacadeOPL facadeOPL = new FacadeOPL(configuracaoOpl, configuracaoCplex, provedorModelo, provedoresDados, null);
		try {
			facadeOPL.executar();
		} catch (Exception e) {
			ExceptionService.reportException(System.err, e);
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void provedorArquivoTest() {
		DataSource provedorDados = new DataSourceFile("nome", new File("src/test/resources/dados/poliedro.dat") );
		Collection<DataSource> provedoresDados = new HashSet<DataSource>();
		provedoresDados.add(provedorDados);
		FacadeOPL facadeOPL = new FacadeOPL(configuracaoOpl, configuracaoCplex, provedorModelo, provedoresDados, null);
		try {
			facadeOPL.executar();
		} catch (Exception e) {
			ExceptionService.reportException(System.err, e);
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void provedorClasspathTest() {
		DataSource provedorDados = new DataSourceClasspath("nome", "/dados/poliedro.dat");
		Collection<DataSource> provedoresDados = new HashSet<DataSource>();
		provedoresDados.add(provedorDados);
		FacadeOPL facadeOPL = new FacadeOPL(configuracaoOpl, configuracaoCplex, provedorModelo, provedoresDados, null);
		try {
			facadeOPL.executar();
		} catch (Exception e) {
			ExceptionService.reportException(System.err, e);
			Assert.fail(e.getMessage());
		}
	}
}
