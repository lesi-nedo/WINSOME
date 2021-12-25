package winServ;

import java.util.Objects;

public class Result implements ResultInterface {
	private boolean result;
	private String reason;
	public Result(boolean result, String reason) {
		this.result=result;
		this.reason=reason;
	}
	
	public boolean getResult() {
		return this.result;
	}
	public String getReason() {
		return this.reason;
	}
	public void setResult(boolean result) {
		this.result=result;
	}
	public void setReason(String reason) {
		this.reason=reason;
	}
	@Override
	public int hashCode() {
		return Objects.hash(this.result, this.reason);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		} else if (!(obj instanceof Result)) {
			return false;
		} else {
			Result other= (Result) obj;
			return Objects.equals(this.result, other.result) &&
					Objects.equals(this.reason, other.reason);
		}
	}
	@Override
	public String toString() {
		return "Result: " + this.result + " Reason: " + this.reason;
	}
}
