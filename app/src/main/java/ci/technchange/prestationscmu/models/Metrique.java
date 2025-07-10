package ci.technchange.prestationscmu.models;

public class Metrique {
    private int id;
    private String activite;
    private String dateDebut;
    private String dateFin;
    private String idFamoco;
    private int idRegion;
    private int statusSynchro;

    public Metrique() {
    }

    public Metrique(String activite, String dateDebut, String dateFin, String idFamoco, int idRegion) {
        this.activite = activite;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.idFamoco = idFamoco;
        this.idRegion = idRegion;
    }

    public Metrique(int id, String activite, String dateDebut, String dateFin, String idFamoco, int idRegion) {
        this.id = id;
        this.activite = activite;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.idFamoco = idFamoco;
        this.idRegion = idRegion;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getActivite() {
        return activite;
    }

    public void setActivite(String activite) {
        this.activite = activite;
    }

    public String getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(String dateDebut) {
        this.dateDebut = dateDebut;
    }

    public String getDateFin() {
        return dateFin;
    }

    public void setDateFin(String dateFin) {
        this.dateFin = dateFin;
    }

    public String getIdFamoco() {
        return idFamoco;
    }

    public void setIdFamoco(String idFamoco) {
        this.idFamoco = idFamoco;
    }

    public int getIdRegion() {
        return idRegion;
    }

    public void setIdRegion(int idRegion) {
        this.idRegion = idRegion;
    }

    public void setStatusSynchro(int statusSynchro){ this.statusSynchro = statusSynchro;}
    public int getStatusSynchro(){return statusSynchro;}

    @Override
    public String toString() {
        return "Metrique{" +
                "id=" + id +
                ", activite='" + activite + '\'' +
                ", dateDebut='" + dateDebut + '\'' +
                ", dateFin='" + dateFin + '\'' +
                ", idFamoco='" + idFamoco + '\'' +
                ", idRegion=" + idRegion +'\''+
                ", statusSynchro=" +statusSynchro+
                '}';
    }
}