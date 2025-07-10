package ci.technchange.prestationscmu.models;

public class MetriqueConnexion {
    private int id;
    private String codeEts;
    private String codeAgac;
    private String nomComplet;
    private String dateConnexion;
    private String heureConnexion;
    private int idRegion;
    private int statusSynchro;


    public MetriqueConnexion() {
    }


    public MetriqueConnexion(String codeEts, String codeAgac, String nomComplet,
                             String dateConnexion, String heureConnexion, int statusSynchro) {
        this.codeEts = codeEts;
        this.codeAgac = codeAgac;
        this.nomComplet = nomComplet;
        this.dateConnexion = dateConnexion;
        this.heureConnexion = heureConnexion;
        this.statusSynchro = statusSynchro;
    }


    public MetriqueConnexion(int id, String codeEts, String codeAgac, String nomComplet,
                             String dateConnexion, String heureConnexion, int idRegion,int statusSynchro) {
        this.id = id;
        this.codeEts = codeEts;
        this.codeAgac = codeAgac;
        this.nomComplet = nomComplet;
        this.dateConnexion = dateConnexion;
        this.heureConnexion = heureConnexion;
        this.idRegion = idRegion;
        this.statusSynchro = statusSynchro;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodeEts() {
        return codeEts;
    }

    public void setCodeEts(String codeEts) {
        this.codeEts = codeEts;
    }

    public String getCodeAgac() {
        return codeAgac;
    }

    public void setCodeAgac(String codeAgac) {
        this.codeAgac = codeAgac;
    }

    public String getNomComplet() {
        return nomComplet;
    }

    public void setNomComplet(String nomComplet) {
        this.nomComplet = nomComplet;
    }

    public String getDateConnexion() {
        return dateConnexion;
    }

    public void setDateConnexion(String dateConnexion) {
        this.dateConnexion = dateConnexion;
    }

    public String getHeureConnexion() {
        return heureConnexion;
    }

    public void setHeureConnexion(String heureConnexion) {
        this.heureConnexion = heureConnexion;
    }

    public int getStatusSynchro() {
        return statusSynchro;
    }


    public void setStatusSynchro(int statusSynchro) {
        this.statusSynchro = statusSynchro;
    }
    public int getIdRegion(){
        return idRegion;
    }
     public void setIdRegion(int idRegion){
        this.idRegion = idRegion;
     }

    // Méthode toString pour le débogage
    @Override
    public String toString() {
        return "MetriqueConnexion{" +
                "id=" + id +
                ", codeEts='" + codeEts + '\'' +
                ", codeAgac='" + codeAgac + '\'' +
                ", nomComplet='" + nomComplet + '\'' +
                ", dateConnexion='" + dateConnexion + '\'' +
                ", heureConnexion='" + heureConnexion + '\'' +
                ", idRegion='"+idRegion+'\''+
                ", statusSynchro=" + statusSynchro +
                '}';
    }
}
