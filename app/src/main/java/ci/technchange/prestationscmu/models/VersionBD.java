package ci.technchange.prestationscmu.models;

public class VersionBD {
    private int id;
    private int numero_version;


    public VersionBD(){
        this.numero_version = 0;
    }

    public int getIdVersion(){return id;}
    public void  setIdVersion(int id){this.id=id;}
    public int getNumeroVersion() { return numero_version; }
    public void setNumeroVersion(int numero_version) { this.numero_version = numero_version; }
}
