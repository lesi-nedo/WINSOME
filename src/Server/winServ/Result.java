package winServ;

import java.util.Objects;

public class Result implements ResultInterface {
	/*
	 * Overview: A wrapper class for the return value
	 */
	private boolean result;
	private String reason;
	//@Effects: initializes the object of type Result
	//@param result: the final outcome
	//@param reason: the cause of the final outcome
	public Result(boolean result, String reason) {
		this.result=result;
		this.reason=reason;
	}
	//@Effects: transmits the outcome
	//@Returns: true if success, false otherwise
	public boolean getResult() {
		return this.result;
	}
	//@Effects: transmits the cause
	//@Returns: the motivation of the result
	public String getReason() {
		return this.reason;
	}
	//@Effects: sets the outcome
	//@param result: true if success, false otherwise
	public void setResult(boolean result) {
		this.result=result;
	}
	//@Effects: sets the cause of the outcome
	//@param reason: the motivation of the this.result
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
		return "Result: " + this.result + ". --- Reason: " + this.reason +".";
	}
}
