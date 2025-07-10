package ci.technchange.prestationscmu.models;

public class Professionel {
   private int id;
   private  String inp, nomcomplet, specialite, region, departement, ville, code_ets, etablissement ;

    public Professionel(){}
    public Professionel(int id, String inp, String nom, String specialite, String region,
                   String departement, String ville, String code_ets, String etablissement) {
        this.id = id;
        this.inp = inp;
        this.nomcomplet = nom;

        this.specialite = specialite;
        this.region = region;
        this.departement = departement;
        this.ville = ville;
        this.code_ets = code_ets;
        this.etablissement = etablissement;
    }

    public Professionel(int id, String inp, String nomcomplet, String specialite,String code_ets, String etablissement) {
        this.id = id;
        this.inp = inp;
        this.nomcomplet = nomcomplet;
        this.specialite = specialite;
        this.code_ets = code_ets;
        this.etablissement = etablissement;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInp() {
        return inp;
    }

    public void setInp(String inp) {
        this.inp = inp;
    }

    public String getNom() {
        return nomcomplet;
    }

    public void setNom(String nom) {
        this.nomcomplet = nom;
    }


    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getDepartement() {
        return departement;
    }

    public void setDepartement(String departement) {
        this.departement = departement;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getCodeEts() {
        return code_ets;
    }

    public void setCodeEts(String code_ets) {
        this.code_ets = code_ets;
    }

    public String getEtablissement() {
        return etablissement;
    }

    public void setEtablissement(String etablissement) {
        this.etablissement = etablissement;
    }

    @Override
    public String toString() {
        return nomcomplet;
    }
}
