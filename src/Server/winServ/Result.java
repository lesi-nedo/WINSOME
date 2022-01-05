package winServ;

import java.util.Objects;
import java.util.StringTokenizer;

public class Result implements ResultInterface {
	/*
	 * Overview: A wrapper class for the return value
	 */
	private int result;
	private String res_str;
	private String reason;
	//@Effects: initializes the object of type Result
	//@param result: the final outcome
	//@param reason: the cause of the final outcome
	public Result(int result, String reason) {
		this.result=result;
		this.reason=reason;
		this.res_str=to_cor_res(result);
	}
	//@Effects: transmits the outcome
	//@Returns: http code
	public int getResult() {
		return this.result;
	}
	//@Effects: transmits the outcome
	//@Returns: http code in format "HTTP/1.1 code meaning"
		public String getResult_Str() {
			return this.res_str;
		}
	//@Effects: transmits the cause
	//@Returns: the motivation of the result
	public String getReason() {
		return this.reason;
	}
	//@Effects: sets the outcome
	//@param result: sets the http code
	public void setResult(int result) {
		this.res_str=to_cor_res(result);
		this.result=result;
	}
	//@Effects: sets the outcome
	//@param result: sets the result
		public void setResult_Str (String res_str) {
			StringTokenizer str_tok = new StringTokenizer(res_str);
			str_tok.nextToken();
			this.result = Integer.valueOf(str_tok.nextToken());
			this.res_str=res_str;
		}
	//@Effects: sets the cause of the outcome
	//@param reason: the motivation of the this.result
	public void setReason(String reason) {
		this.reason=reason;
	}
	//@Effects transforms the http code in a string inf format "HTTP/1.1 code reason"
	//@param code: the http code
	private String to_cor_res(int code) {
		switch(code) {
		case 200:
				return "OK";
		case 201:
				return "Created";
		case 202:
				return "Accepted"; 
		case 204:
				return "No Content";
		case 400:
				return "Bad Request";
		case 401:
				return "Unauthorized";
		case 403:
				return "Forbidden";
		case 404:
				return "Not Found";
		case 409: 
			    return "Conflict";
		case 410:
				return "Gone";
		case 500:
			return "Internal Server Error";
		default:
				return "Not Implemented";
			
		}
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
					Objects.equals(this.reason, other.reason) && 
					Objects.equals(this.res_str, other.res_str);
		}
	}
	@Override
	public String toString() {
		return "HTTP/1.1 "+this.result +" " + this.res_str +"\r\n" + this.reason;
	}
}
