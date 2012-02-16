package infra.slf4j;

public class OperationFactory {
	public static Operation getOperation(String name) {
		return new BasicOperation(name, null);
	}

	public static Operation getOperation(String name, String message) {
		return new BasicOperation(name, message);
	}
}
