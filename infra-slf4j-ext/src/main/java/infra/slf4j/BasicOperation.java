package infra.slf4j;

public class BasicOperation implements OperationWithMessage {
	private final String name;
	private final String message;

	protected BasicOperation(String name, String message) {
		this.name = name;
		this.message = message;
	}
	@Override
	public String getName() { return name; }
	@Override
	public String getMessage() { return message; }
}
