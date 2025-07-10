package ci.technchange.prestationscmu.models;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Patient implements Serializable {
    private final int id;

    @SerializedName("nom")
    private final String nom;

    @SerializedName("prenoms")
    private final String prenoms;

    @SerializedName("date_naissance")
    private final String dateNaissance;

    @SerializedName("telephone")
    private final String telephone;

    @SerializedName("lieu_naissance")
    private final String lieuNaissance;

    @SerializedName("sexe")
    private final String sexe;

    @SerializedName("csp")
    private final String csp;

    @SerializedName("cmr")
    private final String cmr;

    @SerializedName("num_secu")
    private final String numSecu;

    @SerializedName("guid")
    private final String guid;

    @SerializedName("nomjeunefille")
    private final String nomJeuneFille;

    public Patient(int id, String nom, String prenoms, String dateNaissance, String telephone,
                   String lieuNaissance, String sexe, String csp, String cmr,
                   String numSecu, String guid, String nomJeuneFille) {
        this.id = id;
        this.nom = nom;
        this.prenoms = prenoms;
        this.dateNaissance = dateNaissance;
        this.telephone = telephone;
        this.lieuNaissance = lieuNaissance;
        this.sexe = sexe;
        this.csp = csp;
        this.cmr = cmr;
        this.numSecu = numSecu;
        this.guid = guid;
        this.nomJeuneFille = nomJeuneFille;
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenoms() { return prenoms; }
    public String getDateNaissance() { return dateNaissance; }
    public String getTelephone() { return telephone; }
    public String getLieuNaissance() { return lieuNaissance; }
    public String getSexe() { return sexe; }
    public String getCsp() { return csp; }
    public String getCmr() { return cmr; }
    public String getNumSecu() { return numSecu; }
    public String getGuid() { return guid; }
    public String getNomJeuneFille() { return nomJeuneFille; }
}
