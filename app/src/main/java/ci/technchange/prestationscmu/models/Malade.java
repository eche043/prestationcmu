package ci.technchange.prestationscmu.models;

import java.io.Serializable;

public class Malade implements Serializable {
    private int id;
    private String nom;
    private String prenom;
    private String numero;
    private String dateNaissaince;
    private String sexe;
    private String typePiece;
    private String numeropiece;
    private String rectoPath;
    private String versoPath;
    private String photoPath;
    private String dateCreation;



    public Malade(String nom, String prenom, String numero, String dateNaissaince, String sexe, String typePiece, String numeropiece) {
        this.nom = nom;
        this.prenom = prenom;
        this.numero = numero;
        this.dateNaissaince = dateNaissaince;
        this.sexe = sexe;
        this.typePiece = typePiece;
        this.numeropiece = numeropiece;
    }

    // Getters et setters pour tous les champs
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getDateNaissaince() {
        return dateNaissaince;
    }

    public void setDateNaissaince(String dateNaissaince) {
        this.dateNaissaince = dateNaissaince;
    }

    public String getSexe() {
        return sexe;
    }

    public void setSexe(String sexe) {
        this.sexe = sexe;
    }

    public String getTypePiece() {
        return typePiece;
    }

    public void setTypePiece(String typePiece) {
        this.typePiece = typePiece;
    }

    public String getNumeropiece() {
        return numeropiece;
    }

    public void setNumeropiece(String numeropiece) {
        this.numeropiece = numeropiece;
    }

    public String getRectoPath() {
        return rectoPath;
    }

    public void setRectoPath(String rectoPath) {
        this.rectoPath = rectoPath;
    }

    public String getVersoPath() {
        return versoPath;
    }

    public void setVersoPath(String versoPath) {
        this.versoPath = versoPath;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
    }
}