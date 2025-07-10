package ci.technchange.prestationscmu.models;

public class ActeMedical {
    private int id;
    private String typeActe;
    private String code;
    private String libelle;
    private String titre;
    private String coeficient;
    private double tarif;
    private String lettreCle;

    public ActeMedical(int id, String typeActe, String code, String libelle, String titre, double tarif) {
        this.id = id;
        this.typeActe = typeActe;
        this.code = code;
        this.libelle = libelle;
        this.titre = titre;

        this.tarif = tarif;

    }

    public ActeMedical( String code, String libelle, double tarif) {

        this.code = code;
        this.libelle = libelle;
        this.tarif = tarif;
    }
    public ActeMedical() {

    }


    // Getters
    public int getId() {
        return id;
    }

    public String getTypeActe() {
        return typeActe;
    }

    public String getCoeficient(){return coeficient;}
    public String getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getTitre() {
        return titre;
    }

    public double getTarif() {
        return tarif;
    }

    public String getLettreCle() {
        return lettreCle;
    }

    public void setLettreCle(String lettreCle) {
        this.lettreCle = lettreCle;
    }


    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setTypeActe(String typeActe) {
        this.typeActe = typeActe;
    }
    public void setCode(String code) {
        this.code = code;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setTarif(double tarif) {
        this.tarif = tarif;
    }

    public void setCoeficient(String coeficient)
    {this.coeficient =coeficient;}

    @Override
    public String toString() {
        return libelle;
    }
}
