///*
// * Copyright 2012 Daniel Felix Ferber
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package infra.exception.motivo;
//
//import infra.exception.RichException;
//import infra.exception.RichRuntimeException;
//
//
///**
// * Identifica o "motivo" (ou causa) de uma operação não poder ser realizada. Normalmente é uma falha que faz parte da
// * lógica da operação e que precisa ser reportada. De acordo com o motivo reportado, a aplicação pode decidir se realiza
// * uma segunta tentativa da operação, ou se deve recuperar o erro.
// * <p>
// * O uso de {@link MotivoException} ou {@link MotivoRuntimeException} contendo a referência para {@link Motivo} é uma
// * forma sucinta de identificar todas as possíveis falhas sem precisar criar uma classe que especializa Exception para
// * cada motivo possível.
// * <p>
// * É interessante declarar o motivo como uma enumeração que implementa {@link Motivo}. Desta forma é possível agrupar
// * motivos relacionados. E é possível realizar uma ação sobre motivos agrupados (fazendo um instanceof de acordo com o
// * enum que agrupa os motivos).
// *
// * @author Daniel Felix Ferber (x7ws) - Grupo de Pesquisa Operacional
// *
// * @deprecated This class shall be replaced by {@link RichException} or preferably by {@link RichRuntimeException}, since most
// * exception patterns were better described as runtime.
// */
//@Deprecated
//public interface Motivo {
//	/**
//	 * Uma descrição que documenta o motivo. Não necessariamente esta mensagem deve ser mostrada para o usuário.
//	 */
//	String getMensagem();
//
//	/**
//	 * Uma descrição da operação que gera um erro com este motivo.
//	 */
//	String getOperacao();
//}
