package infra.ilog.opl.dados;

import static infra.exception.Assert.Argument;
import ilog.opl.IloOplModel;

import java.io.IOException;


public abstract class DataSinkJava extends AbstractDataSink {
	public DataSinkJava(String nome) {
		super(nome);
	}

	@Override
	public void consumeData(IloOplModel oplModel) throws IOException {
		Argument.notNull(oplModel);
		translateFromOpl(oplModel);
	}

	protected abstract void translateFromOpl(IloOplModel oplModel);
}
