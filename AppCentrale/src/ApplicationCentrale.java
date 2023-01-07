import java.sql.*;
import java.util.Scanner;

public class ApplicationCentrale {

    private final Scanner scanner = new Scanner(System.in);

    private Connection connection;

    private PreparedStatement ajouterCours;
    private PreparedStatement ajouterEtudiant;
    private PreparedStatement inscrireEtudiantAuCours;
    private PreparedStatement ajouterProjet;
    private PreparedStatement creerGroupes;
    private PreparedStatement visualiserCours;
    private PreparedStatement visualiserProjets;
    private PreparedStatement visualiserGroupesProjet;
    private PreparedStatement validerGroupeProjet;
    private PreparedStatement validerGroupesProjet;


    public ApplicationCentrale() throws SQLException {
            String url = "jdbc:postgresql://127.0.0.1:5432/db";
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver PostgreSQL manquant !");
            System.exit(1);
        }
        try {
            connection = DriverManager.getConnection(url, "user", "password");

            ajouterCours = connection.prepareStatement("SELECT projet.ajouter_cours(?, ?, ?, ?)");
            ajouterEtudiant = connection.prepareStatement("SELECT projet.ajouter_etudiant(?, ?, ?, ?)");
            inscrireEtudiantAuCours = connection.prepareStatement("SELECT projet.inscrire_etudiant(?, ?)");
            ajouterProjet = connection.prepareStatement("SELECT projet.ajouter_projet(?, ?, ?, ?, ?)");
            creerGroupes = connection.prepareStatement("SELECT projet.ajouter_groupes(?, ?, ?)");
            visualiserCours = connection.prepareStatement("SELECT * FROM projet.visualiser_cours");
            visualiserProjets = connection.prepareStatement("SELECT * FROM projet.visualiser_projets");
            visualiserGroupesProjet = connection.prepareStatement("SELECT * FROM projet.visualiser_groupes WHERE identifiant_projet = ?");
            validerGroupeProjet = connection.prepareStatement("SELECT projet.valider_groupe(?, ?)");
            validerGroupesProjet = connection.prepareStatement("SELECT projet.valider_tous_les_groupes(?)");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    public void start() throws SQLException {
        int choix = 0;
        while (true) {
            System.out.println("============================ Application Centrale ============================");
            System.out.println("1. Ajouter un cours");
            System.out.println("2. Ajouter un étudiant");
            System.out.println("3. Inscrire un étudiant à un cours");
            System.out.println("4. Créer un projet pour un cours");
            System.out.println("5. Créer des groupes pour un projet");
            System.out.println("6. Visualiser les cours avec ses projets");
            System.out.println("7. Visualiser tous les projets");
            System.out.println("8. Visualiser toutes les compositions de groupes d'un projet");
            System.out.println("9. Valider un groupe");
            System.out.println("10. Valider tous les groupes d'un projet");
            System.out.println("11. Fermer l'application");
            System.out.println("==============================================================================");
            System.out.print("Entrez votre choix: ");

            try {
                choix = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Erreur: Veuillez entrer un nombre");
                continue;
            }

            if (choix == 0) break;

            if (choix < 0 || choix > 11) {
                System.out.println("Erreur: Veuillez entrer un nombre entre 1 et 11");
                continue;
            }

            switch (choix) {
                case 1:
                    ajouterCours();
                    break;
                case 2:
                    ajouterEtudiant();
                    break;
                case 3:
                    inscrireEtudiant();
                    break;
                case 4:
                    creerProjet();
                    break;
                case 5:
                    creerGroupes();
                    break;
                case 6:
                    visualiserCours();
                    break;
                case 7:
                    visualiserProjets();
                    break;
                case 8:
                    visualiserGroupes();
                    break;
                case 9:
                    validerGroupe();
                    break;
                case 10:
                    validerGroupes();
                    break;
                case 11:
                    System.out.println("Fermeture de l'application");
                    this.close();
                    System.exit(0);
                    break;
                default:
                    break;
            }
        }
    }

    private void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void validerGroupes() {
        System.out.println("============================ Valider tous les groupes d'un projet ============================");
        System.out.print("Entrez le code du projet: ");
        String codeProjet = scanner.nextLine();

        try {
            validerGroupesProjet.setString(1, codeProjet);
            validerGroupesProjet.execute();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("Tous les groupes du projet " + codeProjet + " ont été validés");
        System.out.println("==============================================================================================");
    }

    private void validerGroupe() {
        System.out.println("================================= Valider un groupe ==============================");
        System.out.print("Entrez l'id du projet: ");
        String idProjet = scanner.nextLine();

        int numeroGroupe = 0;
        try {
            System.out.print("Entrez le numero du groupe: ");
            numeroGroupe = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Erreur: Veuillez entrer un nombre");
            validerGroupes();
        }

        try {
            validerGroupeProjet.setString(1, idProjet);
            validerGroupeProjet.setInt(2, numeroGroupe);

            validerGroupeProjet.execute();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("Le groupe a été validé avec succès");
        System.out.println("==================================================================================");
    }

    private void visualiserGroupes() {
        System.out.println("========================== Visualiser groupes d'un projet =======================");
        System.out.print("Entrez le code du projet: ");
        String codeProjet = scanner.nextLine();

        try {
            visualiserGroupesProjet.setString(1, codeProjet);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        try (ResultSet resultSet = visualiserGroupesProjet.executeQuery()) {
            while (resultSet.next()) {
                System.out.println("Numéro: " + resultSet.getInt("numero_groupe") + "\tNom: " + resultSet.getString("nom") + "\tPrenom: " + resultSet.getString("prenom") + "\tComplet: " + resultSet.getBoolean("complet") + "\tValide: " + resultSet.getBoolean("valide"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void visualiserProjets() {
        System.out.println("========================= Visualiser tous les projets ==========================");
        try (ResultSet resultSet = visualiserProjets.executeQuery()) {
            while (resultSet.next()) {
                System.out.println("=============================== Projet " + resultSet.getInt("identifiant_projet") + " =========================================");
                System.out.println("Nom: " + resultSet.getString("nom"));
                System.out.println("Code cours: " + resultSet.getString("code_cours"));
                System.out.println("Nombre de groupes: " + resultSet.getInt("nbr_groupes"));
                System.out.println("Nombre de groupes complets: " + resultSet.getInt("nbr_groupes_complet"));
                System.out.println("Nombre de groupes validés: " + resultSet.getInt("nbr_groupes_valide"));
                System.out.println("==================================================================================");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void visualiserCours() {
        System.out.println("======================= Visualiser les cours avec ses projets ========================");
        try (ResultSet resultSet = visualiserCours.executeQuery()) {
            while (resultSet.next()) {
                System.out.println("===============================" + resultSet.getString("code_cours") + "=========================================");
                System.out.println("Nom: " + resultSet.getString("nom"));
                System.out.println("Projets: " + resultSet.getInt("coalesce"));
                System.out.println("==================================================================================");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void creerGroupes() {
        System.out.println("================================= Créer des groupes =================================");
        System.out.println("Entrer l'id du projet: ");
        String idProjet = scanner.nextLine();

        int nbGroupes = 0;
        int nbEtudiants = 0;
        try {
            System.out.println("Entrer le nombre de groupes: ");
            nbGroupes = Integer.parseInt(scanner.nextLine());
            System.out.println("Entrer le nombre d'étudiants par groupe: ");
            nbEtudiants = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Erreur: Veuillez entrer un nombre");
            creerGroupes();
        }

        try {
            creerGroupes.setString(1, idProjet);
            creerGroupes.setInt(2, nbGroupes);
            creerGroupes.setInt(3, nbEtudiants);

            creerGroupes.execute();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("Groupes créés avec succès");
        System.out.println("===================================================================================");
    }

    private void creerProjet() {
        System.out.println("================================ Création d'un projet =============================");
        System.out.println("Entrez l'identifiant du projet: ");
        String idProjet = scanner.nextLine();

        System.out.println("Entrez le nom du projet: ");
        String nomProjet = scanner.nextLine();

        System.out.println("Entrez la date de début du projet (YYYY-MM-DD): ");
        Date dateDebut = Date.valueOf(scanner.nextLine());

        System.out.println("Entrez la date de fin du projet (YYYY-MM-DD): ");
        Date dateFin = Date.valueOf(scanner.nextLine());

        System.out.println("Entrez le code du cours: ");
        String codeCours = scanner.nextLine();

        try {
            ajouterProjet.setString(1, idProjet);
            ajouterProjet.setString(2, nomProjet);
            ajouterProjet.setDate(3, dateDebut);
            ajouterProjet.setDate(4, dateFin);
            ajouterProjet.setString(5, codeCours);

            ajouterProjet.execute();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("Projet ajouté avec succès");
        System.out.println("==================================================================================");
    }

    private void inscrireEtudiant() {
        System.out.println("================================ Inscrire un étudiant =============================");
        System.out.println("Entrez l'email de l'étudiant: ");
        String email = scanner.nextLine();
        System.out.println("Entrez le code du cours: ");
        String code = scanner.nextLine();

        try {
            inscrireEtudiantAuCours.setString(1, email);
            inscrireEtudiantAuCours.setString(2, code);

            inscrireEtudiantAuCours.execute();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("L'étudiant a été inscrit au cours avec succès!");
        System.out.println("==================================================================================");
    }

    private void ajouterEtudiant() {
        System.out.println("================================ Ajouter un étudiant ==============================");
        System.out.println("Entrez le nom de l'étudiant: ");
        String nom = scanner.nextLine();
        System.out.println("Entrez le prénom de l'étudiant: ");
        String prenom = scanner.nextLine();
        System.out.println("Entrez l'email de l'étudiant: ");
        String email = scanner.nextLine();
        System.out.println("Entrez le mot de passe de l'étudiant: ");
        String motDePasse = scanner.nextLine();

        try {
            String sel = BCrypt.gensalt();
            motDePasse = BCrypt.hashpw(motDePasse, sel);

            ajouterEtudiant.setString(1, nom);
            ajouterEtudiant.setString(2, prenom);
            ajouterEtudiant.setString(3, email);
            ajouterEtudiant.setString(4, motDePasse);

            ajouterEtudiant.execute();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("L'étudiant a été ajouté avec succès");
        System.out.println("==================================================================================");
    }

    private void ajouterCours() {
        System.out.println("================================ Ajouter un cours =================================");
        System.out.println("Entrez le code du cours: ");
        String codeCours = scanner.nextLine();
        System.out.println("Entrez le nom du cours: ");
        String nomCours = scanner.nextLine();

        int blocCours = 0;
        int creditsCours = 0;
        try {
            System.out.println("Entrez le bloc du cours: ");
            blocCours = Integer.parseInt(scanner.nextLine());
            System.out.println("Entrez le nombre de crédits du cours: ");
            creditsCours = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Erreur: Veuillez entrer un nombre");
            ajouterCours();
        }

        try {
            ajouterCours.setString(1, codeCours);
            ajouterCours.setString(2, nomCours);
            ajouterCours.setInt(3, blocCours);
            ajouterCours.setInt(4, creditsCours);

            ajouterCours.execute();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("Le cours a été ajouté avec succès");
        System.out.println("===================================================================================");
    }
}
