package dps.common;

public class ProductionLineInfo {
    private int id;
    private String address;
    private int port;

    public ProductionLineInfo() {

    }

    public ProductionLineInfo(int id, String address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
    }

    public int getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "Line " + id + " with address " + address + ": " + port;
    }
}
