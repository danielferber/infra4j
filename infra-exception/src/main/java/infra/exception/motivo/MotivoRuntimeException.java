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
package infra.exception.motivo;

/**
 * Exceção que encapsula um {@link Motivo} e uma {@link Exception} que detalha o {@link Motivo}.
 * Métodos públicos que realizam uma operação devem lançar esta exceção ao invés de
 * {@link Exception} específicas, para que seja possível ao chamador tomar uma ação específica para
 * cada possível motivo da falha.
 *
 * @author Daniel Felix Ferber (x7ws) - Grupo de Pesquisa Operacional
 */
@Deprecated
public class MotivoRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final Motivo motivo;

	public MotivoRuntimeException(Motivo motivo, Object... args) {
		super(String.format(motivo.getMensagem(), args));
		this.motivo = motivo;
	}

	public MotivoRuntimeException(Exception causa, Motivo motivo, Object... args) {
		super(String.format(motivo.getMensagem(), args), causa);
		this.motivo = motivo;
	}

	public Motivo getMotivo() { return motivo; };

	public String getCodigoMotivo() {
		return MotivoException.codigoMotivo(motivo);
	}
}
