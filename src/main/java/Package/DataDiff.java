package Package;

public class DataDiff {

    public int row;
    public String column;
    public Object d1;
    public Object d2;

    public DataDiff() {}

    public DataDiff(int row, String column, Object d1, Object d2) {
        this.row = row;
        this.column = column;
        this.d1 = d1;
        this.d2 = d2;
    }
}
