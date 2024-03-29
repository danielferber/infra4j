package infra.ilog.opl.dados;

import ilog.opl.IloCustomOplDataSource;
import ilog.opl.IloOplDataHandler;
import ilog.opl.IloOplFactory;
import ilog.opl.IloOplModel;

import java.io.IOException;


public abstract class DataSourceJava extends AbstractDataSource {
	public DataSourceJava(String nome) {
		super(nome);
	}

	@Override
	public void produceData(IloOplModel oplModel) throws IOException {
		IloOplFactory oplFactory = IloOplFactory.getOplFactoryFrom(oplModel);
		oplModel.addDataSource(new InternalDataSouce(oplFactory));
	}

	public class InternalDataSouce extends IloCustomOplDataSource {
		public InternalDataSouce(IloOplFactory oplFactory) {
			super(oplFactory);
		}

		@Override
		public void customRead() {
			translateToOpl(getDataHandler());
		}
	}

	protected abstract void translateToOpl(IloOplDataHandler dataHandler);
}

