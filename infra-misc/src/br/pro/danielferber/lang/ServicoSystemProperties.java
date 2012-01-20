package br.pro.danielferber.lang;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Redefine as propriedades do sistema. A plataforma Java pode obter uma propriedade do sistema
 * operacional com um valor vazio ou incorreto. Neste caso será necessário utillizar esta classe na
 * início da aplicação para ler um arquivo de configuração com as propriedades corrigidas
 * manualmente. Um caso típico é "user.timezone", que pode ser obtido com o fuso horário errado no
 * Windows XP.
 * <p>
 * <b>Para depurar as propriedades do sistema utilizadas pela JVM:</b> Inicie a aplicação com
 * <code>-DServicoSystemProperties.debug=true</code>. As propriedades de sistema são impressas em
 * <code>stdout</code> com o conteúdo que será utilizado pela JVM.
 * <p>
 * <b>Para redefinir as propriedades do sistema:</b> Crie um arquivo de properties que define os
 * valores corretos para as chaves. Escreva este arquivo com nome <i>system.properties</i> no
 * diretório de onde a aplicação é iniciada, no <i>diretório config/system.properties</i> em relação
 * ao diretório de onde a aplicação é iniciada ou no arquivo especificado por
 * <code>-DServicoSystemProperties.path=/caminho/para/arquivo</code>.
 * <p>
 * <b>Obs: </b>Só é possível re-definir propriedades que começam com "<code>user.</code>".
 * 
 * @author Daniel Felix Ferber
 */
public class ServicoSystemProperties {
	private static final String PREFIXO = ServicoSystemProperties.class.getSimpleName();
	public static final String PROPERTY_PATH = PREFIXO + ".path";
	public static final String PROPERTY_DEBUG = PREFIXO + ".debug";

	public static void instalar() {
		final boolean debug = Boolean.parseBoolean(System.getProperty(PROPERTY_DEBUG));
		final String path = System.getProperty(PROPERTY_PATH);
		final PrintStream o = System.err;
		final String prefixo = "System properties: ";
		if (debug) {
			o.print(prefixo);
			o.println("Conteúdo original:");
			System.getProperties().list(o);
			o.flush();
		}
		/*
		 * Estratégia para encontrar o arquivo: 1) indicado pelo argumento -D na linha de comando 2)
		 * current dir/config/system.properties 3) current dir/system.properties
		 */
		File file = null;
		if (path != null) {
			file = new File(path).getAbsoluteFile();
		} else {
			file = new File("config/system.properties").getAbsoluteFile();
			if (!file.exists()) {
				file = new File("system.properties").getAbsoluteFile();
			}
			if (!file.exists()) {
				file = null;
			}
		}
		if (file == null) {
			if (debug) {
				o.print(prefixo);
				o.println("Não modificar.");
			}
			return;
		}

		/*
		 * Tentar ler arquivo e redefinir propriedades.
		 */
		if (debug) {
			o.print(prefixo);
			o.format("Tentar arquivo %s.\n", file.getAbsolutePath());
		}
		Properties properties = new Properties();
		FileInputStream is = null;
		try {
			is = new FileInputStream(file);
			properties.load(is);
			@SuppressWarnings("unchecked")
			Enumeration<String> names = (Enumeration<String>) properties.propertyNames();
			while (names.hasMoreElements()) {
				String name = (String) names.nextElement();
				if (name.startsWith("user.")) {
					String antes = System.getProperty(name);
					String depois = properties.getProperty(name);
					System.setProperty(name, depois);
					if (debug) {
						o.print(prefixo);
						o.format("%s=%s (antes =%s).\n", name, depois, antes);
					}
				} else {
					if (debug) {
						o.print(prefixo);
						o.format("ignorado: %s.\n", name);
					}
				}
			}
		} catch (IOException e) {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException ee) {
				// ignora
			} finally {
				is = null;
			}
			throw new RuntimeException("Não foi possível abrir arquivo para redefinir system properties.", e);
		}
	}

}
