package advdb;

public class DataItem {
	int value;
	int writeTime;	// default -1, when create on siteManager
	String varName;
	boolean isRead;

	public DataItem(String varName, int val, boolean isRead, int writeTime) {
		this.value = val;
		this.varName = varName;
		this.isRead = isRead;
		this.writeTime = writeTime;
	}
	
	public boolean isRead() {
		return this.isRead;
	}
	public void setIsRead(boolean isRead) {
		this.isRead = isRead;
	}
	
	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public String getVarName() {
		return varName;
	}

	public void setVarName(String varName) {
		this.varName = varName;
	}
	
	public int getWriteTime() {
		return writeTime;
	}
}
