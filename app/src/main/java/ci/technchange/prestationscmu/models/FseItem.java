package ci.technchange.prestationscmu.models;

import android.os.Parcel;
import android.os.Parcelable;

public class FseItem implements Parcelable {
    private String transactionNumber;
    private String securityNumber;
    private String fullName;
    private String status; // "Terminé" ou "En cours"
    private String TypeSoins;
    private String DateNaissance;
    private String Etablissement;
    private String sexe; // Nouvel élément
    private boolean etablissementCmr;
    private boolean etablismentAutre;
    private boolean etablismentRef;
    private boolean etablissmentEloignement;
    private boolean etablissementUrgent;
    private String Guid;
    private int fseId;
    private String dateSoins;
    private String codeEts;
    private String numFsInitial;
    private String code_acte1;
    private String code_acte2;
    private String code_acte3;
    private String montant_acte;
    private String part_cmu;
    private String part_assure;



    public FseItem(String transactionNumber, String securityNumber, String fullName, String status, String TypeSoins, String DateNaissance, String Etablissement, int fseId, String dateSoins) {
        this.transactionNumber = transactionNumber;
        this.securityNumber = securityNumber;
        this.fullName = fullName;
        this.status = status;
        this.TypeSoins = TypeSoins;
        this.DateNaissance = DateNaissance;
        this.Etablissement = Etablissement;
        this.fseId = fseId;
        this.code_acte1 = "";
        this.code_acte2 = "";
        this.code_acte3 = "";
        this.montant_acte = "0";
        this.part_cmu = "0";
        this.part_assure = "0";
        this.dateSoins = dateSoins;
    }


    public FseItem() {
        this.code_acte1 = "";
        this.code_acte2 = "";
        this.code_acte3 = "";
        this.montant_acte = "0";
        this.part_cmu = "0";
        this.part_assure = "0";
    }


    public String getTransactionNumber() { return transactionNumber; }
    public String getSecurityNumber() { return securityNumber; }
    public String getFullName() { return fullName; }
    public String getStatus() { return status; }
    public String getTypesoins() { return TypeSoins; }
    public String getDateNaissance() { return DateNaissance; }
    public String getEtablissement() { return Etablissement; }
    public String getSexe() { return sexe; } // Getter pour sexe
    public boolean isEtablissementCmr() { return etablissementCmr; }
    public boolean isEtablismentAutre(){return  etablismentAutre;}
    public boolean isEtablismentRef(){return etablismentRef;}
    public boolean isEtablissmentEloignement(){return  etablissmentEloignement;}
    public boolean isEtablissementUrgent(){return  etablissementUrgent;}
    public int getFseId() { return fseId; }
    public String getGuid(){return Guid;}
    public String getDateSoins() { return dateSoins; }
    public String getCodeEts() { return codeEts; }
    public String getNumFsInitial() { return numFsInitial; }
    public String getCode_acte1() { return code_acte1; }
    public String getCode_acte2() { return code_acte2; }
    public String getCode_acte3() { return code_acte3; }
    public String getMontant_acte() { return montant_acte; }
    public String getPart_cmu() { return part_cmu; }
    public String getPart_assure() { return part_assure; }

    // Setters
    public void setTransactionNumber(String transactionNumber) {
        this.transactionNumber = transactionNumber;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setSecurityNumber(String securityNumber) {
        this.securityNumber = securityNumber;
    }

    public void setDateNaissance(String DateNaissance) {
        this.DateNaissance = DateNaissance;
    }

    public void setEtablissement(String Etablissement) {
        this.Etablissement = Etablissement;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTypeSoins(String TypeSoins) {
        this.TypeSoins = TypeSoins;
    }

    public void setFseId(int fseId) {
        this.fseId = fseId;
    }

    public void setSexe(String sexe) {
        this.sexe = sexe;
    }

    public void setGuid(String Guid){
        this.Guid = Guid;
    }

    public void setEtablissementCmr(boolean etablissementCmr) {
        this.etablissementCmr = etablissementCmr;
    }
    public void setEtablismentAutre(boolean etablismentAutre){
        this.etablismentAutre = etablismentAutre;
    }
    public void setEtablismentRef(boolean etablismentRef){
        this.etablismentRef = etablismentRef;
    }
    public void setEtablissmentEloignement(boolean etablissmentEloignement){
        this.etablissmentEloignement = etablissmentEloignement;
    }
    public void setEtablissementUrgent(boolean etablissementUrgent){
        this.etablissementUrgent = etablissementUrgent;
    }

    public void setDateSoins(String dateSoins) {
        this.dateSoins = dateSoins;
    }

    public void setCodeEts(String codeEts) {
        this.codeEts = codeEts;
    }

    public void setNumFsInitial(String numFsInitial) {
        this.numFsInitial = numFsInitial;
    }

    public void setCode_acte1(String code_acte1) {
        this.code_acte1 = code_acte1;
    }

    public void setCode_acte2(String code_acte2) {
        this.code_acte2 = code_acte2;
    }

    public void setCode_acte3(String code_acte3) {
        this.code_acte3 = code_acte3;
    }

    public void setMontant_acte(String montant_acte) {
        this.montant_acte = montant_acte;
    }

    public void setPart_cmu(String part_cmu) {
        this.part_cmu = part_cmu;
    }

    public void setPart_assure(String part_assure) {
        this.part_assure = part_assure;
    }

    // Parcelable implementation
    protected FseItem(Parcel in) {
        transactionNumber = in.readString();
        securityNumber = in.readString();
        fullName = in.readString();
        status = in.readString();
        TypeSoins = in.readString();
        DateNaissance = in.readString();
        Etablissement = in.readString();
        sexe = in.readString();
        etablissementCmr = in.readByte() != 0;
        etablissmentEloignement = in.readByte() != 0;
        etablismentRef = in.readByte() != 0;
        etablismentAutre = in.readByte() != 0;
        etablissementUrgent = in.readByte() != 0;
        fseId = in.readInt();
        Guid = in.readString();
        dateSoins = in.readString();
        codeEts = in.readString();
        numFsInitial = in.readString();
        code_acte1 = in.readString();
        code_acte2 = in.readString();
        code_acte3 = in.readString();
        montant_acte = in.readString();
        part_cmu = in.readString();
        part_assure = in.readString();
    }

    public static final Creator<FseItem> CREATOR = new Creator<FseItem>() {
        @Override
        public FseItem createFromParcel(Parcel in) {
            return new FseItem(in);
        }

        @Override
        public FseItem[] newArray(int size) {
            return new FseItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(transactionNumber);
        dest.writeString(securityNumber);
        dest.writeString(fullName);
        dest.writeString(status);
        dest.writeString(TypeSoins);
        dest.writeString(DateNaissance);
        dest.writeString(Etablissement);
        dest.writeString(sexe);
        dest.writeByte((byte) (etablissementCmr ? 1 : 0));
        dest.writeByte((byte) (etablissmentEloignement ? 1 : 0));
        dest.writeByte((byte) (etablismentRef ? 1 : 0));
        dest.writeByte((byte) (etablismentAutre ? 1 : 0));
        dest.writeByte((byte) (etablissementUrgent ? 1 : 0));
        dest.writeInt(fseId);
        dest.writeString(Guid);
        dest.writeString(dateSoins);
        dest.writeString(codeEts);
        dest.writeString(numFsInitial);
        dest.writeString(code_acte1);
        dest.writeString(code_acte2);
        dest.writeString(code_acte3);
        dest.writeString(montant_acte);
        dest.writeString(part_cmu);
        dest.writeString(part_assure);
    }
}