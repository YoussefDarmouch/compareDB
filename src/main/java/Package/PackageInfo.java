package Package;

public class PackageInfo {

    private String name;
    private String specification;
    private String body;

    public PackageInfo() {}

    public PackageInfo(String name, String specification, String body) {
        this.name = name;
        this.specification = specification;
        this.body = body;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public static String normalize(String code) {
        if (code == null) return "";
        return code.replaceAll("\\s+", "").toLowerCase();
    }

    public boolean specSameAs(PackageInfo other) {
        return normalize(this.specification).equals(normalize(other.specification));
    }

    public boolean bodySameAs(PackageInfo other) {
        return normalize(this.body).equals(normalize(other.body));
    }
}
