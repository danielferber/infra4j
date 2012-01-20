package br.pro.danielferber.infra.pojo;

public abstract class ObjetoComID {
	private Long id;

	public final Long getId() { return id; }
	protected final void setId(Long id) { this.id = id; }

	protected ObjetoComID() {
		super();
	}
	
	@Override
	public boolean equals(Object obj) {
		/*
		 * Só faz sentido comparar IDs se forem objetos de exatamente mesma classe.
		 * Só faz sentido se os dois objetos possuem um ID diferente de null.
		 */
		if (! obj.getClass().equals(this.getClass())) throw new ClassCastException();
		if (this.getId() == null || ((ObjetoComID)obj).getId() == null) throw new NullPrimaryKeyException();
		if (this == obj) return true;
		return this.getId() == ((ObjetoComID)obj).getId();
	}

	@Override
	public int hashCode() {
		if (this.getId() == null) throw new NullPrimaryKeyException();
		return id.hashCode();
	}
}
