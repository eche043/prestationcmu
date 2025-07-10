package ci.technchange.prestationscmu.models;

public class FseAmbulatoire {
    private int id;
    private String numTrans;
    private String numSecu;
    private String numGuid;
    private String nomComplet;
    private String sexe;
    private String dateNaissance;
    private String nomEtablissement;
    private String codeEts;
    private String date_soins;
    private boolean etablissementCmr;
    private boolean etablissementUrgent;
    private boolean etablissementRef;
    private boolean etablissementEloignement;
    private String etablissementPrecision;
    private String professionnel;
    private String codeProfessionnel;
    private String speProfessionnel;
    private String codeAff1;
    private String codeAff2;
    private boolean infoMaternite;
    private boolean infoAVP;
    private boolean infoATMP;
    private boolean infoAutre;
    private int statusProgres;
    private boolean preInscription;
    private String urlPhoto;
    private String type_fse;
    private String numFsInitial;

    private int statusEnvoie;
    private String date_synchro;

    private String motif;
    private String codeAffection;
    private int nombre_jour;
    private String code_acte1;
    private String code_acte2;
    private String code_acte3;
    private String quantite_1;
    private String quantite_2;
    private String quantite_3;
    private String montant_acte;
    private String part_cmu;
    private String part_assure;

    public FseAmbulatoire() {
        this.numTrans = "";
        this.numSecu = "";
        this.numGuid = "";
        this.nomComplet = "";
        this.sexe = "";
        this.dateNaissance = "";
        this.nomEtablissement = "";
        this.codeEts = "";
        this.date_soins = "";
        this.etablissementCmr = false;
        this.etablissementUrgent = false;
        this.etablissementRef = false;
        this.etablissementEloignement = false;
        this.etablissementPrecision = "";
        this.professionnel = "";
        this.codeProfessionnel = "";
        this.speProfessionnel = "";
        this.codeAff1 = "";
        this.codeAff2 = "";
        this.infoMaternite = false;
        this.infoAVP = false;
        this.infoATMP = false;
        this.infoAutre = false;
        this.statusProgres = 0;
        this.preInscription = false;
        this.code_acte1 = "";
        this.code_acte2 = "";
        this.code_acte3 = "";
        this.quantite_1 ="1";
        this.quantite_2 ="1";
        this.quantite_3 ="1";
        this.montant_acte = "0";
        this.part_cmu = "0";
        this.part_assure = "0";
    }


    public FseAmbulatoire(int id, String numTrans, String numSecu, String numGuid, String nomComplet,
                          String sexe, String dateNaissance, String nomEtablissement, String codeEts, String date_soins,
                          boolean etablissementCmr, boolean etablissementUrgent, boolean etablissementRef,
                          boolean etablissementEloignement, String etablissementPrecision,
                          String professionnel, String codeProfessionnel, String speProfessionnel,
                          String codeAff1, String codeAff2, boolean infoMaternite, boolean infoAVP,
                          boolean infoATMP, boolean infoAutre,
                          int statusProgres, boolean preInscription, String TypeFse, String quantite_1, String quantite_2, String quantite_3) {
        this.id = id;
        this.numTrans = numTrans;
        this.numSecu = numSecu;
        this.numGuid = numGuid;
        this.nomComplet = nomComplet;
        this.sexe = sexe;
        this.dateNaissance = dateNaissance;
        this.nomEtablissement = nomEtablissement;
        this.codeEts = codeEts;
        this.date_soins = date_soins;
        this.etablissementCmr = etablissementCmr;
        this.etablissementUrgent = etablissementUrgent;
        this.etablissementRef = etablissementRef;
        this.etablissementEloignement = etablissementEloignement;
        this.etablissementPrecision = etablissementPrecision;
        this.professionnel = professionnel;
        this.codeProfessionnel = codeProfessionnel;
        this.speProfessionnel = speProfessionnel;
        this.codeAff1 = codeAff1;
        this.codeAff2 = codeAff2;
        this.infoMaternite = infoMaternite;
        this.infoAVP = infoAVP;
        this.infoATMP = infoATMP;
        this.infoAutre = infoAutre;
        this.statusProgres = statusProgres;
        this.preInscription = preInscription;
        this.motif = null;
        this.nombre_jour = 0;
        this.codeAffection = null;
        this.type_fse = TypeFse;
        this.code_acte1 = "";
        this.code_acte2 = "";
        this.code_acte3 = "";
        this.quantite_1 ="1";
        this.quantite_2="1";
        this.quantite_3="1";
        this.montant_acte = "0";
        this.part_cmu = "0";
        this.part_assure = "0";
    }

    public FseAmbulatoire(int id, String numTrans, String numSecu, String numGuid, String nomComplet,
                          String sexe, String dateNaissance, String nomEtablissement, String codeEts, String date_soins,
                          boolean etablissementCmr, boolean etablissementUrgent, boolean etablissementRef,
                          boolean etablissementEloignement, String etablissementPrecision,
                          String professionnel, String codeProfessionnel, String speProfessionnel,
                          String codeAff1, String codeAff2, boolean infoMaternite, boolean infoAVP,
                          boolean infoATMP, boolean infoAutre,
                          int statusProgres, boolean preInscription, String motif, String code_aff, int nbjr) {
        this.id = id;
        this.numTrans = numTrans;
        this.numSecu = numSecu;
        this.numGuid = numGuid;
        this.nomComplet = nomComplet;
        this.sexe = sexe;
        this.dateNaissance = dateNaissance;
        this.nomEtablissement = nomEtablissement;
        this.codeEts = codeEts;
        this.date_soins = date_soins;
        this.etablissementCmr = etablissementCmr;
        this.etablissementUrgent = etablissementUrgent;
        this.etablissementRef = etablissementRef;
        this.etablissementEloignement = etablissementEloignement;
        this.etablissementPrecision = etablissementPrecision;
        this.professionnel = professionnel;
        this.codeProfessionnel = codeProfessionnel;
        this.speProfessionnel = speProfessionnel;
        this.codeAff1 = codeAff1;
        this.codeAff2 = codeAff2;
        this.infoMaternite = infoMaternite;
        this.infoAVP = infoAVP;
        this.infoATMP = infoATMP;
        this.infoAutre = infoAutre;
        this.statusProgres = statusProgres;
        this.preInscription = preInscription;
        this.codeAffection = code_aff;
        this.motif = motif;
        this.nombre_jour = nbjr;
        this.code_acte1 = "";
        this.code_acte2 = "";
        this.code_acte3 = "";
        this.montant_acte = "0";
        this.part_cmu = "0";
        this.part_assure = "0";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNumTrans() {
        return numTrans;
    }

    public void setNumTrans(String numTrans) {
        this.numTrans = numTrans;
    }

    public String getNumSecu() {
        return numSecu;
    }

    public void setNumSecu(String numSecu) {
        this.numSecu = numSecu;
    }

    public String getNumGuid() {
        return numGuid;
    }

    public void setNumGuid(String numGuid) {
        this.numGuid = numGuid;
    }

    public String getNomComplet() {
        return nomComplet;
    }

    public void setNomComplet(String nomComplet) {
        this.nomComplet = nomComplet;
    }

    public String getDate_soins() {
        return date_soins;
    }

    public void setDate_soins(String date_soins) {
        this.date_soins = date_soins;
    }

    public String getSexe() {
        return sexe;
    }

    public void setSexe(String sexe) {
        this.sexe = sexe;
    }

    public String getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(String dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getNomEtablissement() {
        return nomEtablissement;
    }

    public void setNomEtablissement(String nomEtablissement) {
        this.nomEtablissement = nomEtablissement;
    }

    public String getCodeEts() {
        return codeEts;
    }

    public void setCodeEts(String codeEts) {
        this.codeEts = codeEts;
    }

    public boolean isEtablissementCmr() {
        return etablissementCmr;
    }

    public void setEtablissementCmr(boolean etablissementCmr) {
        this.etablissementCmr = etablissementCmr;
    }

    public boolean isEtablissementUrgent() {
        return etablissementUrgent;
    }

    public void setEtablissementUrgent(boolean etablissementUrgent) {
        this.etablissementUrgent = etablissementUrgent;
    }

    public boolean isEtablissementRef() {
        return etablissementRef;
    }

    public void setEtablissementRef(boolean etablissementRef) {
        this.etablissementRef = etablissementRef;
    }

    public boolean isEtablissementEloignement() {
        return etablissementEloignement;
    }

    public void setEtablissementEloignement(boolean etablissementEloignement) {
        this.etablissementEloignement = etablissementEloignement;
    }

    public String getEtablissementPrecision() {
        return etablissementPrecision;
    }

    public void setEtablissementPrecision(String etablissementPrecision) {
        this.etablissementPrecision = etablissementPrecision;
    }

    public String getProfessionnel() {
        return professionnel;
    }

    public void setProfessionnel(String professionnel) {
        this.professionnel = professionnel;
    }

    public String getCodeProfessionnel() {
        return codeProfessionnel;
    }

    public void setCodeProfessionnel(String codeProfessionnel) {
        this.codeProfessionnel = codeProfessionnel;
    }

    public String getSpeProfessionnel() {
        return speProfessionnel;
    }

    public void setSpeProfessionnel(String speProfessionnel) {
        this.speProfessionnel = speProfessionnel;
    }

    public String getCodeAff1() {
        return codeAff1;
    }

    public void setCodeAff1(String codeAff1) {
        this.codeAff1 = codeAff1;
    }

    public String getCodeAff2() {
        return codeAff2;
    }

    public void setCodeAff2(String codeAff2) {
        this.codeAff2 = codeAff2;
    }

    public boolean isInfoMaternite() {
        return infoMaternite;
    }

    public void setInfoMaternite(boolean infoMaternite) {
        this.infoMaternite = infoMaternite;
    }

    public boolean isInfoAVP() {
        return infoAVP;
    }

    public void setInfoAVP(boolean infoAVP) {
        this.infoAVP = infoAVP;
    }

    public boolean isInfoATMP() {
        return infoATMP;
    }

    public void setInfoATMP(boolean infoATMP) {
        this.infoATMP = infoATMP;
    }

    public boolean isInfoAutre() {
        return infoAutre;
    }

    public void setInfoAutre(boolean infoAutre) {
        this.infoAutre = infoAutre;
    }

    public int iStatusProgres() {
        return statusProgres;
    }

    public void setStatusProgres(int statusProgres) {
        this.statusProgres = statusProgres;
    }

    public String getNumFsInitial() {
        return numFsInitial;
    }

    public void setNumFsInitial(String numFsInitial) {
        this.numFsInitial = numFsInitial;
    }

    public boolean isPreInscription() {
        return preInscription;
    }

    public void setPreInscription(boolean preInscription) {
        this.preInscription = preInscription;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public String getType_fse() {
        return type_fse;
    }

    public void setType_fse(String typeFse) {
        this.type_fse = typeFse;
    }

    public int getNombre_jour() {
        return nombre_jour;
    }

    public void setNombre_jour(int nombre_jour) {
        this.nombre_jour = nombre_jour;
    }

    public String getCodeAffection() {
        return codeAffection;
    }

    public void setCodeAffection(String codeAffection) {
        this.codeAffection = codeAffection;
    }

    public int getStatusEnvoie() {
        return statusEnvoie;
    }

    public void setStatusEnvoie(int statusEnvoie) {
        this.statusEnvoie = statusEnvoie;
    }

    public String getDate_synchro() {
        return date_synchro;
    }

    public void setDate_synchro(String date_synchro) {
        this.date_synchro = date_synchro;
    }

    public String getUrlPhoto() {
        return urlPhoto;
    }

    public void setUrlPhoto(String urlPhoto) {
        this.urlPhoto = urlPhoto;
    }

    public String getCode_acte1() {
        return code_acte1;
    }

    public void setCode_acte1(String code_acte1) {
        this.code_acte1 = code_acte1;
    }

    public String getCode_acte2() {
        return code_acte2;
    }

    public void setCode_acte2(String code_acte2) {
        this.code_acte2 = code_acte2;
    }

    public String getCode_acte3() {
        return code_acte3;
    }

    public String getQuantite_1(){ return quantite_1;}
    public void setQuantite_1(String quantite_1){ this.quantite_1 = quantite_1;}
    public String getQuantite_2(){ return quantite_2;}
    public void setQuantite_2(String quantite_2){ this.quantite_2 = quantite_2;}
    public String getQuantite_3(){ return quantite_3;}
    public void setQuantite_3(String quantite_3){ this.quantite_3 = quantite_3;}

    public void setCode_acte3(String code_acte3) {
        this.code_acte3 = code_acte3;
    }

    public String getMontant_acte() {
        return montant_acte;
    }

    public void setMontant_acte(String montant_acte) {
        this.montant_acte = montant_acte;
    }

    public String getPart_cmu() {
        return part_cmu;
    }

    public void setPart_cmu(String part_cmu) {
        this.part_cmu = part_cmu;
    }

    public String getPart_assure() {
        return part_assure;
    }

    public void setPart_assure(String part_assure) {
        this.part_assure = part_assure;
    }

    @Override
    public String toString() {
        return "FseAmbulatoire{" +
                "id=" + id +
                "numFsInitial=" + numFsInitial + '\'' +
                ", numTrans='" + numTrans + '\'' +
                ", numSecu='" + numSecu + '\'' +
                ", numGuid='" + numGuid + '\'' +
                ", nomComplet='" + nomComplet + '\'' +
                ", sexe='" + sexe + '\'' +
                ", dateNaissance='" + dateNaissance + '\'' +
                ", nomEtablissement='" + nomEtablissement + '\'' +
                ", codeEts='" + codeEts + '\'' +
                ", date_soins='" + date_soins + '\'' +
                ", etablissementCmr=" + etablissementCmr +
                ", etablissementUrgent=" + etablissementUrgent +
                ", etablissementRef=" + etablissementRef +
                ", etablissementEloignement=" + etablissementEloignement +
                ", etablissementPrecision='" + etablissementPrecision + '\'' +
                ", professionnel='" + professionnel + '\'' +
                ", codeProfessionnel='" + codeProfessionnel + '\'' +
                ", speProfessionnel='" + speProfessionnel + '\'' +
                ", codeAff1='" + codeAff1 + '\'' +
                ", codeAff2='" + codeAff2 + '\'' +
                ", infoMaternite=" + infoMaternite +
                ", infoAVP=" + infoAVP +
                ", infoATMP=" + infoATMP +
                ", infoAutre=" + infoAutre +
                ", statusProgres=" + statusProgres +
                ", preInscription=" + preInscription +
                ", code_acte1='" + code_acte1 + '\'' +
                ", code_acte2='" + code_acte2 + '\'' +
                ", code_acte3='" + code_acte3 + '\'' +
                ", montant_acte='" + montant_acte + '\'' +
                ", part_cmu='" + part_cmu + '\'' +
                ", part_assure='" + part_assure + '\'' +
                '}';
    }
}