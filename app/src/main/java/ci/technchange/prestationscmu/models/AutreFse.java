package ci.technchange.prestationscmu.models;

public class AutreFse {
    private int id;
    private String numTransaction;
    private String urlPhoto;


    public AutreFse() {
        this.id = id;
        this.numTransaction = numTransaction;
        this.urlPhoto = urlPhoto;

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNumTransaction() {
        return numTransaction;
    }

    public void setNumTrans(String numTransaction) {
        this.numTransaction = numTransaction;
    }

    public String getUrlPhoto(){return  urlPhoto;}

    public void setUrlPhoto(String urlPhoto){this.urlPhoto = urlPhoto;}

    @Override
    public String toString() {
        return "Metrique{" +
                "id=" + id +
                ", numTransaction='" + numTransaction + '\'' +
                ", urlPhoto='" + urlPhoto + '\'' +
                '}';
    }
}
