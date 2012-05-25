package br.pro.danielferber.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Um mecanismo bem simples para injetar configurações atribuídas por outros módulos.
 * As configurações são informadas como propriedades de sistema ou como conteúdo em arquivos no classpath.
 * <p>
 * Esta foi a alternativa encontrada para prover um mecanismo de configurações durante o bootstrap.
 * O uso de um framework específico de configurações durante um bootstrap causa falhas aleatórias e não rastreáveis.
 * <p>
 * A configuração é conjunto de pares chave-valor. 
 * A chave é um identificador formado por um prefixo comum e um sufixo que é o nome da chave.
 * O prefixo comum agrupa as configurações para separ=á-las em módulos.
 * O valor é um cadeia de caracteres.
 * <p>
 * Os pares que formam uma configuração podem ser informados de duas formas:
 * <ol>
 * <li>Através de propriedade de sistema, deve-se utilizar -Dprefixo.sufixo=valor na linha de comando.
 * <li>Através de arquivo no classpath, localizado em /prefixo/sufixo e com uma única linha texto contendo o valor.
 * </ul>
 * A primeira forma tem prioridade sobre a segunda. Em caso de omissão de uma chave nas duas formas, ou é assumido um valor padrão,
 * ou é lançada uma exceção RuntimeException.
 * @author Daniel Felix Ferber
 */
public class Configuracao {
	/** Prefixo comum do nome das chaves desta configuração. */
	final String prefixo;

	/** Construtor padrão.
	 * @param prefixo Prefixo comum do nome das chaves desta configuração.
	 */
	public Configuracao(String prefixo) {
		if (prefixo == null) throw new IllegalArgumentException("prefixo == null");
		this.prefixo = prefixo;
	}

	/**
	 * Obtém o valor de uma propriedade. 
	 * @param sufixo Nome da chave que identifica a propriedade, ou seja, o sufixo.
	 * @param padrao Valor padrão para a propriedade, caso seja omitida na configuração. <code>null</code> quando não se aplica um valor padrão.
	 * @return Cadeia de caracteres que representa o valor.
	 * @throws RuntimeException caso ocorra um erro ao obter a propriedade.
	 */
	public String getProperty(String sufixo, String padrao) {
		if (sufixo == null) throw new IllegalArgumentException("sufixo == null");
		
		String chave = prefixo + "." + sufixo;
		String caminho = "/" + prefixo + "/" + sufixo;
		String conteudo = System.getProperty(chave);
		if (conteudo == null) {
			InputStream is = Configuracao.class.getResourceAsStream(caminho);
			if (is != null) {
				conteudo = copyToString(is).trim();
			} 
		}
		if (conteudo == null) {
			conteudo = padrao;
		}
		return conteudo;
	}

	/**
	 * Obtém uma instância de objeto como valor da propriedade.
	 * O objeto é criado usando o construtor padrão (sem argumentos) da classe informada como valor da propriedade.  
	 * @param sufixo Nome da chave que identifica a propriedade, ou seja, o sufixo.
	 * @return Objeto definido pela extensão.
	 * @throws RuntimeException caso ocorra um erro ao obter a extensão.
	 */
	public Object getObjeto(String sufixo, String padrao) {
		String nomeClasse = getProperty(sufixo, padrao);

		Class<?> classe;
		try {
			classe = Configuracao.class.getClassLoader().loadClass(nomeClasse);
		} catch (Exception e) {
			throw new RuntimeException("Classe "+nomeClasse+" não encontrada no classpath.", e);
		}

		Object objeto;
		try {
			objeto = classe.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Falha ao criar instância da classe"+nomeClasse+".", e);
		}
		return objeto;
	}

	/** 
	 * Lê todo conteúdo de um {@link InputStream} e o retorna como string.
	 * <p>
	 * Pressupõe que o conteúdo seja uma pequena, pois não considera questões de desempenho e memória.
	 * 
	 * @param is {@link InputStream} para ser lido.
	 * @return Conteúdo do {@link InputStream}.
	 * @throws RuntimeException caso ocorra um erro na leitura.
	 */
	private static String copyToString(InputStream is) {
		InputStreamReader isr = new InputStreamReader(is);
		char cb[] = new char[100];
		StringBuilder sb = new StringBuilder();
		try {
			int count = isr.read(cb);
			while (count != -1) {
				sb.append(cb, 0, count);
				count = isr.read(cb);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return sb.toString();
	}

}
