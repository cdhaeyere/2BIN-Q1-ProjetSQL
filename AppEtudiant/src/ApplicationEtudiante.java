import java.sql.*;
import java.util.Scanner;

public class ApplicationEtudiante {
    private Connection connection;
    private Scanner scanner = new Scanner(System.in);
    private PreparedStatement visualiserCoursEtudiant;
    private PreparedStatement ajouterEtudiantGroupe;
    private PreparedStatement retirerEtudiantGroupe;
    private PreparedStatement visualiserProjetsEtudiant;
    private PreparedStatement visualiserProjetsEtudiantSansGroupe;
    private PreparedStatement visualiserGroupesIncomplets;
    private PreparedStatement inscrire;
    private PreparedStatement connecter;
    private int idEtudiant;

    public ApplicationEtudiante() {
        String url = "jdbc:postgresql://127.0.0.1:5432/db";

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver PostgreSQL manquant !");
            System.exit(1);
        }

        try {
            connection = DriverManager.getConnection(url,"user","password");

            visualiserCoursEtudiant = connection.prepareStatement("SELECT * FROM projet.visualiser_cours_etudiant WHERE id_etudiant = ?");
            ajouterEtudiantGroupe = connection.prepareStatement("SELECT projet.ajouter_etudiant_groupe(?,?,?)");
            retirerEtudiantGroupe = connection.prepareStatement("SELECT projet.retirer_etudiant_groupe(?,?)");
            visualiserProjetsEtudiant = connection.prepareStatement("SELECT * FROM projet.visualiser_projets_etudiant WHERE id_etudiant = ?");
            visualiserProjetsEtudiantSansGroupe = connection.prepareStatement("SELECT * FROM projet.visualiser_projets_etudiant_sans_groupe WHERE id_etudiant = ?");
            visualiserGroupesIncomplets = connection.prepareStatement("SELECT * FROM projet.visualiser_groupes_incomplets WHERE identifiant_projet = ?");
            connecter = connection.prepareStatement("SELECT projet.connecter_etudiant(?)");

        } catch (SQLException e){
            System.out.println("Erreur de connexion a la base de données");
            System.exit(1);
        }
    }

    public void start() {
        int option = 0;

        while (true) {
            System.out.println("==================== Application Etudiante ====================");
            System.out.println("1. Se connecter");
            System.out.println("2. Fermer l'application");
            System.out.println("==============================================");
            System.out.println("Entrez votre choix: ");

            try {
                option = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Erreur de saisie");
                continue;
            }

            if (option == 0) break;

            if (option < 0 || option > 2) {
                System.out.println("Erreur: Veuillez entrer un nombre entre 1 et 2");
                continue;
            }

            switch (option) {
                case 1: {
                    seConnecter();
                    break;
                }
                case 2: {
                    System.out.println("Fermeture de l'application");
                    this.close();
                    System.exit(0);
                    break;
                }
            }
        }
    }

    private void seConnecter() {
        System.out.println("Entrez votre e-mail :");
        String e_mail = scanner.nextLine();

        System.out.println("Entrez votre mot de passe :");
        String mdp = scanner.nextLine();

        try {
            connecter.setString(1,e_mail);
        } catch (SQLException e) {
            System.out.println("Erreur lors de la connexion de l'etudiant");
        }

        try (ResultSet resultSet = connecter.executeQuery()) {
            if (resultSet.next()) {
                if (BCrypt.checkpw(mdp, resultSet.getString("connecter_etudiant").split(",")[1].replace(")",""))) {
                    idEtudiant = Integer.parseInt(resultSet.getString("connecter_etudiant").split(",")[0].replace("(",""));
                    menu();
                } else {
                    System.out.println("mots de passe ou e-mail invalide");
                    seConnecter();
                }
            } else {
                System.out.println("mots de passe ou e-mail invalide");
                seConnecter();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void menu(){
        int option;
        while (true) {
            System.out.println("==================== Menu ====================");
            System.out.println("1. Visualiser mes cours");
            System.out.println("2. Intégrer un groupe");
            System.out.println("3. Quitter un groupe");
            System.out.println("4. Visualiser mes projets");
            System.out.println("5. Visualiser les projets sans groupe");
            System.out.println("6. Visualiser les groupes incomplets");
            System.out.println("7. Fermer l'application");
            System.out.println("==============================================");
            System.out.println("Entrez votre choix: ");

            try {
                option = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Erreur: Veuillez entrer un nombre");
                continue;
            }

            if (option == 0) break;

            if (option < 0 || option > 7) {
                System.out.println("Erreur: Veuillez entrer un nombre entre 1 et 7");
                continue;
            }

            switch (option){
                case 1: {
                    visualiserCour();
                    break;
                }
                case 2: {
                    insererEtudiantGroupe();
                    break;
                }
                case 3: {
                    quitterGroupe();
                    break;
                }
                case 4: {
                    visualiserProjets();
                    break;
                }
                case 5: {
                    visualiserProjetSansGroupe();
                    break;
                }
                case 6: {
                    visualiserGroupesNonComplets();
                    break;
                }
                case 7: {
                    System.out.println("Fermeture de l'application");
                    this.close();
                    System.exit(0);
                    break;
                }
                default:
                    break;
            }
        }
    }

    private void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            System.out.println("Erreur lors de la fermeture de la connexion");
        }
    }

    private void visualiserCour() {
        System.out.println("========================== Visualiser mes cours =======================");
        try {
            visualiserCoursEtudiant.setInt(1,idEtudiant);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        try (ResultSet resultSet = visualiserCoursEtudiant.executeQuery()) {
            while (resultSet.next()) {
                System.out.println("Code: " + resultSet.getString(2) + ", Nom: " + resultSet.getString(3) + ", Projets: " + resultSet.getString(4));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void insererEtudiantGroupe() {
        System.out.println("================================= Intégrer un groupe ==============================");
        System.out.println("Numero du projet: ?");
        String idProjet = scanner.nextLine();

        int numGroupe = 0;
        try {
            System.out.println("Numero du groupe: ?");
            numGroupe = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Erreur: Veuillez entrer un nombre");
            return;
        }
        try {
            ajouterEtudiantGroupe.setInt(1,idEtudiant);
            ajouterEtudiantGroupe.setString(2,idProjet);
            ajouterEtudiantGroupe.setInt(3,numGroupe);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        try (ResultSet resultSet = ajouterEtudiantGroupe.executeQuery()) {
            if (resultSet.next()){
                System.out.println("ajout réussi");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void quitterGroupe() {
        System.out.println("================================= Quitter un groupe ==============================");
        System.out.println("Numero du projet: ?");
        String idProjet = scanner.nextLine();

        try {
            retirerEtudiantGroupe.setInt(1,idEtudiant);
            retirerEtudiantGroupe.setString(2,idProjet);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        try (ResultSet resultSet = retirerEtudiantGroupe.executeQuery()) {
            if (resultSet.next()){
                System.out.println("Suppression réussie");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void visualiserProjets() {
        System.out.println("================================= Visualiser mes projets ==============================");

        try {
            visualiserProjetsEtudiant.setInt(1, idEtudiant);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        try (ResultSet resultSet = visualiserProjetsEtudiant.executeQuery()) {
            while (resultSet.next()){
                System.out.println("Identifiant du projet: " + resultSet.getString(2) + ", Nom du projet: " + resultSet.getString(3) + ",  Code du cour: " + resultSet.getString(4) + ",  Numero du groupe: " + resultSet.getInt(5));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void visualiserProjetSansGroupe() {
        System.out.println("============================ Visualiser les projets sans groupe ==============================");

        try {
            visualiserProjetsEtudiantSansGroupe.setInt(1,idEtudiant);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        try (ResultSet resultSet = visualiserProjetsEtudiantSansGroupe.executeQuery()) {
            while (resultSet.next()){
                System.out.println("Identifiant du projet: " + resultSet.getString(2) + ", Nom du projet: " + resultSet.getString(3) + ",  Code du cour: " + resultSet.getString(4) + ",  Date de début: " + resultSet.getDate(5) + ",  Date de fin: " + resultSet.getDate(6));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void visualiserGroupesNonComplets() {
        System.out.println("============================ Visualiser les groupes incomplets ==============================");
        System.out.println("Numero du projet: ?");
        String idProjet = scanner.nextLine();

        try {
            visualiserGroupesIncomplets.setString(1,idProjet);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        try (ResultSet resultSet = visualiserGroupesIncomplets.executeQuery()) {
            while (resultSet.next()){
                System.out.println("Identifiant du projet: " + resultSet.getString(1) + ", Numero groupe: " + resultSet.getInt(2) + ", Nom: " + resultSet.getString(3) + ",  Prénom: " + resultSet.getString(4) + ",  Nombre de places restantes: " + resultSet.getInt(5));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
