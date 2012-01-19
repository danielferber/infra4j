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
/**
 * Constitui uma forma padrão de reportar exceções.
 * <p/>
 * O método {@link infra.exception.ServicoExcecao#reportarException(java.io.PrintStream, Throwable)} é
 * sugerido como uma forma padrão para reportar exceções para as quais não foi previsto um tratamento adequado pela aplicação.
 * Este método deve ser utilizado principalmente para erros que não foram previstos, como bugs, ou situações muito incomuns para
 * as quais não vale a pena gastar esforço para prover um tratamento adequado.
 * <p/>
 * Por conveniência, os métodos {@link infra.exception.ServicoExcecao#instalar()} define na JVM um handler padrão de exceção
 * que reporta uma exceção não tratada através do método  {@link infra.exception.ServicoExcecao#reportarException(java.io.PrintStream, Throwable)}
 * e registra a exceção em um log dedicado para exceções não tratadas.
 * <p/>
 * Para threads criadas manualmente, pode ser interessante chamar {@link infra.exception.ServicoExcecao#setUncaughtExceptionHandler()}
 * ou {@link infra.exception.ServicoExcecao#setUncaughtExceptionHandler(Thread)} para instalar tal handler padrão.
 * <p/>
 * Por convenção, existem dois tipos de exceções: declaradas e não declaradas
 * (Exception e RuntimeException). Erros para os quais faz sentido tratá-los
 * devem ser lançados como Exception. Erros imprevistos, para os quais não é
 * possível escrever um tratamento adequado, devem ser lançados como
 * RuntimeException.
 * <p/>
 * De preferência, quando a chamada de método é dentro da mesma API, então a
 * propagação de exceção pode ser por Exception (erros previstos) ou
 * RuntimeException (erros imprevistos).
 * <p/>
 * Mas para métodos que são pontos de entrada da API, sugere-se fortemente empregar {@link MotivoException} ao invés de Exception.
 * Desta forma, uma eventual falha pode ser identificada através de um {@link java.lang.Enum}, o que possui várias vantagens.
 * É possível determinar exatamente o motivo específico da falha através do valor do {@link java.lang.Enum}.
 * Não há necessidade de criar inúmeras classes de exception, uma para cada possível motivo de falha.
 * O escopo de um enum é fechado, não é possível adicionar mais valores a um {@link java.lang.Enum} existente.
 * Desta forma, o chamador pode prever exatamente as todas as possíveis causas, sem ser surpreendido por uma causa não conhecida.
 * Exception determina apenas a raiz da hierarquia de classes e não é possível saber em tempo de compilação quais serão as
 * classes que extendem a classe raiz. Isto introduz fragilidade no tratamento de exceção.
 * @author Daniel Felix Ferber (x7ws) - Grupo de Pesquisa Operacional - Grupo de Pesquisa Operacional
 **/
package infra.exception;
