/**
 * Provê seqüência de bootstrap para executar uma aplicação Java como um Serviço Windows/Deamon Unix.
 * <p>
 * A aplicação deve informar a classe que implementa {@link br.pro.danielferber.bootstrap.BootstrapHandler}, 
 * responsável por executar a inicialização e a finalização do servidor. Deve fornecer também uma propriedade 
 * 'bootstrap.handler=nomeQualificadoClasse'. Para aprender como informar uma propriedade para o bootstrap, 
 * consulte {@link br.pro.danielferber.bootstrap.Configuracao}.
 * <p>
 * O bootstrap escreve mensagens de depuração no stdout. Para controlar o detalhamento destas mensagens, deve-se utilizar
 * propriedades 'bootstrap.logger.nomeSimplesClasse=level', onde 'nomeSimplesClasse' é o nome simples de uma das classes 
 * declaradas neste package e level é um dos valores definidos em {@link java.util.logging.Level}.
 * <p>
 * O bootstrap é controlado por uma máquina de estados implementada em {@link br.pro.danielferber.bootstrap.CicloVida}, 
 * que obriga transições corretas start-stop.
 * <p>
 * O bootstrap possui sua própria implementação minimalista de injeção de configuração/dependência e de logging.
 * A configuração do bootstrap é obtida através de {@link br.pro.danielferber.bootstrap.Configuracao}, 
 * que é uma implementação minimalista de injeção de dependências e de configurações.
 * Mensagens de depuração são geradas por {@link br.pro.danielferber.bootstrap.Logger}, 
 * que é uma implementação minimalista de logging.
 * Observou-se que o emprego de frameworks específicos causavam erros aleatório e não rastreáveis durante o bootstrap.
 */
package br.pro.danielferber.bootstrap;
