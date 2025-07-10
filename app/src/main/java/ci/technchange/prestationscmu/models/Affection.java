package ci.technchange.prestationscmu.models;



public class Affection {


    private int id;

    private String codeAffection;

    private String libelle;

    // Constructeurs
    public Affection() {
    }

    public Affection(String codeAffection, String libelle) {
        this.codeAffection = codeAffection;
        this.libelle = libelle;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodeAffection() {
        return codeAffection;
    }

    public void setCodeAffection(String codeAffectation) {
        this.codeAffection = codeAffectation;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    // Méthode toString pour affichage

    public String toStringDebug() {
        return "Affection{" +
                "id=" + id +
                ", codeAffectation='" + codeAffection + '\'' +
                ", libelle='" + libelle + '\'' +
                '}';
    }
    @Override
    public String toString() {
        return libelle;
    }


}

