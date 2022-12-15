DROP SCHEMA IF EXISTS projet CASCADE;

CREATE SCHEMA projet;

CREATE TABLE projet.cours
(
    code_cours CHAR(8) NOT NULL PRIMARY KEY CHECK ( code_cours SIMILAR TO 'BINV[0-9]{4}'),
    nom VARCHAR NOT NULL CHECK ( nom <> '' ),
    bloc INTEGER NOT NULL CHECK ( bloc IN (1, 2, 3)),
    nbr_credits INTEGER NOT NULL CHECK ( nbr_credits > 1 )
);

CREATE TABLE projet.etudiants
(
    id_etudiant  SERIAL  NOT NULL PRIMARY KEY,
    nom          VARCHAR NOT NULL CHECK ( nom <> '' ),
    prenom       VARCHAR NOT NULL CHECK ( prenom <> '' ),
    email        VARCHAR NOT NULL UNIQUE CHECK ( email <> '' AND email SIMILAR TO '%@student.vinci.be'),
    mot_de_passe VARCHAR NOT NULL CHECK ( mot_de_passe <> '' )
);

CREATE TABLE projet.inscriptions
(
    id_etudiant INTEGER    NOT NULL REFERENCES projet.etudiants (id_etudiant),
    code_cours  CHAR(8) NOT NULL REFERENCES projet.cours (code_cours),
    PRIMARY KEY (id_etudiant, code_cours)
);

CREATE TABLE projet.projets
(
    identifiant_projet VARCHAR NOT NULL PRIMARY KEY,
    nom                VARCHAR NOT NULL CHECK ( nom <> '' ),
    date_debut         DATE    NOT NULL,
    date_fin           DATE    NOT NULL CHECK ( date_fin > date_debut ),
    code_cours         CHAR(8) NOT NULL REFERENCES projet.cours (code_cours)
);

CREATE TABLE projet.groupes
(
    nbr_membres_actuel INTEGER NOT NULL DEFAULT 0 CHECK ( nbr_membres_actuel >= 0 ),
    nbr_places         INTEGER NOT NULL CHECK ( nbr_places > 0 ),
    complet            BOOLEAN NOT NULL DEFAULT FALSE,
    valide             BOOLEAN NOT NULL DEFAULT FALSE,
    id_projet          VARCHAR NOT NULL REFERENCES projet.projets (identifiant_projet),
    numero_groupe      INTEGER NOT NULL CHECK ( numero_groupe > 0 ),
    PRIMARY KEY (id_projet, numero_groupe)
);

CREATE TABLE projet.groupes_etudiants
(
    id_etudiant INTEGER NOT NULL REFERENCES projet.etudiants (id_etudiant),
    id_projet   VARCHAR NOT NULL,
    numero      INTEGER NOT NULL,
    PRIMARY KEY (id_projet, id_etudiant),
    FOREIGN KEY (id_projet, numero) REFERENCES projet.groupes (id_projet, numero_groupe)
);


/*
 * ========================================================================
 * Application centrale
 * ========================================================================
 */

-- Operation 1
/**
 * Fonction permettant de créer un nouveau cours
 */
CREATE OR REPLACE FUNCTION projet.ajouter_cours(_code_cours CHAR(8), _nom VARCHAR, _bloc INTEGER, _nbr_credits INTEGER) RETURNS VOID AS $$
BEGIN
    INSERT INTO projet.cours (code_cours, nom, bloc, nbr_credits) VALUES ($1, $2, $3, $4);
END;
$$ LANGUAGE plpgsql;

-- Operation 2
/**
 * Fonction permettant de créer un nouvel étudiant
 */
CREATE OR REPLACE FUNCTION projet.ajouter_etudiant(_nom VARCHAR, _prenom VARCHAR, _email VARCHAR, _mot_de_passe VARCHAR) RETURNS VOID AS $$
BEGIN
    IF EXISTS(SELECT * FROM projet.etudiants e WHERE e.nom = $1 AND e.prenom = $2) THEN
        RAISE EXCEPTION 'L''étudiant % % existe déjà.', $1, $2;
    END IF;
    IF EXISTS(SELECT * FROM projet.etudiants e WHERE e.email = $3) THEN
        RAISE EXCEPTION 'L''email % est déja utilisé.', $3;
    END IF;
    INSERT INTO projet.etudiants (nom, prenom, email, mot_de_passe)
    VALUES ($1, $2, $3, $4);
END;
$$ LANGUAGE plpgsql;

-- Operation 3
/**
 * Fonction permettant d''inscrire un étudiant à un cours
 */
CREATE OR REPLACE FUNCTION projet.inscrire_etudiant(_email VARCHAR, _code_cours CHAR(8)) RETURNS VOID AS
$$
BEGIN
    INSERT INTO projet.inscriptions (id_etudiant, code_cours)
    VALUES ((SELECT id_etudiant FROM projet.etudiants e WHERE e.email = $1), $2);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION projet.inscrire_etudiant_trigger() RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS(SELECT * FROM projet.projets pr WHERE pr.code_cours = NEW.code_cours) THEN
        RAISE EXCEPTION 'Le cours % est déjà associé à un projet.', NEW.code_cours;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER inscrire_etudiant_trigger
    BEFORE INSERT ON projet.inscriptions
    FOR EACH ROW
    EXECUTE PROCEDURE projet.inscrire_etudiant_trigger();

-- Operation 4
/**
 * Fonction permettant de créer un nouveau projet
 */
CREATE OR REPLACE FUNCTION projet.ajouter_projet(_identifiant_projet VARCHAR, _nom VARCHAR, _date_debut DATE, _date_fin DATE, _code_cours CHAR(8)) RETURNS VOID AS $$
BEGIN
    INSERT INTO projet.projets (identifiant_projet, nom, date_debut, date_fin, code_cours) VALUES ($1, $2, $3, $4, $5);
END;
$$ LANGUAGE plpgsql;

-- Operation 5
/**
 * Fonction permettant de créer des groupes pour un projet
 */
CREATE OR REPLACE FUNCTION projet.ajouter_groupes (_id_projet VARCHAR, _nbr_groupes_projet INTEGER, _nbr_places_par_groupe INTEGER) RETURNS VOID AS $$
DECLARE
    _numero_groupe INTEGER := 1;
    _nbr_places_actuel INTEGER := 0;
    groupe RECORD;
BEGIN
    IF (_nbr_groupes_projet < 1) THEN
        RAISE EXCEPTION 'Le nombre de groupes à créer est négatif';
    END IF;
    IF (_nbr_places_par_groupe < 1) THEN
        RAISE EXCEPTION 'Le nombre de places par groupe est négatif';
    END IF;
    IF NOT EXISTS (SELECT * FROM projet.projets p WHERE p.identifiant_projet = $1) THEN
        RAISE EXCEPTION 'Le projet % n''existe pas.', $1;
    END IF;
    IF EXISTS (SELECT * FROM projet.groupes g WHERE g.id_projet = $1) THEN
        FOR groupe IN SELECT * FROM projet.groupes g WHERE g.id_projet = _id_projet LOOP
        _nbr_places_actuel := _nbr_places_actuel + groupe.nbr_places;
    END LOOP;
    END IF;
    IF (((_nbr_groupes_projet * _nbr_places_par_groupe) + _nbr_places_actuel) > (SELECT COUNT(*) FROM projet.inscriptions i WHERE i.code_cours = (SELECT code_cours FROM projet.projets p WHERE p.identifiant_projet = $1))) THEN
        RAISE EXCEPTION 'Vous ne pouvez pas avoir plus de place dans les groupes que de personnes inscrites';
    END IF;

    -- On récupère le dernier numéro de groupe pour le projet
    IF (SELECT COUNT(*) FROM projet.groupes g WHERE g.id_projet = $1) > 0 THEN
        _numero_groupe := (SELECT MAX(g.numero_groupe) FROM projet.groupes g WHERE g.id_projet = $1) + 1;
    END IF;

    FOR i IN 1.._nbr_groupes_projet LOOP
        INSERT INTO projet.groupes (numero_groupe, nbr_places, nbr_membres_actuel, complet, valide, id_projet) VALUES (_numero_groupe, $3, 0, FALSE, FALSE, $1);
        _numero_groupe := _numero_groupe + 1;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Operation 6
/**
 * Visualiser les cours
 */
CREATE OR REPLACE VIEW projet.visualiser_cours AS
    SELECT c.code_cours, c.nom, coalesce(string_agg(p.identifiant_projet, ', '), 'Pas encore de projets')
    FROM projet.cours c
        LEFT JOIN projet.projets p
        ON p.code_cours = c.code_cours
    GROUP BY c.code_cours, c.nom;

-- Operation 7
/**
 * Visualiser les projets
 */
CREATE OR REPLACE VIEW projet.visualiser_projets AS
    SELECT p.identifiant_projet, p.nom, p.code_cours, count(g.numero_groupe) AS nbr_groupes, sum(case when g.complet = true then 1 else 0 end) AS nbr_groupes_complet, sum(case when g.valide = true then 1 else 0 end) AS nbr_groupes_valide
    FROM projet.projets p
        LEFT JOIN projet.groupes g
        ON g.id_projet = p.identifiant_projet
    GROUP BY p.identifiant_projet;

-- Operation 8
/**
 * Visualiser les groupes d'un projet
 */
CREATE VIEW projet.visualiser_groupes AS
SELECT p.identifiant_projet, g.numero_groupe, e.nom, e.prenom, g.complet, g.valide
FROM projet.projets p, projet.groupes g
    LEFT JOIN projet.groupes_etudiants ge
             on g.numero_groupe = ge.numero
                 and g.id_projet = ge.id_projet
    LEFT JOIN projet.etudiants e
             on e.id_etudiant = ge.id_etudiant
WHERE p.identifiant_projet = g.id_projet
ORDER BY g.numero_groupe;

-- Operation 9
/*
 * Fonction permettant de valider un groupe, la validation échoue si le groupe n'est pas complet
 */
CREATE OR REPLACE FUNCTION projet.valider_groupe(_id_projet VARCHAR, _numero_groupe INTEGER) RETURNS VOID AS $$
BEGIN
    IF NOT EXISTS (SELECT * FROM projet.groupes gr WHERE gr.id_projet = _id_projet AND gr.numero_groupe = _numero_groupe) THEN
        RAISE EXCEPTION 'Le groupe n''existe pas';
    END IF;
    IF ((SELECT gr.complet FROM projet.groupes gr WHERE gr.id_projet=_id_projet AND gr.numero_groupe=_numero_groupe) = false) THEN
        RAISE EXCEPTION 'Le groupe % du projet % n''est pas complet.', _numero_groupe, _id_projet;
    END IF;
    UPDATE projet.groupes g SET valide = true WHERE g.id_projet = $1 AND g.numero_groupe = $2;
END;
$$ LANGUAGE plpgsql;

-- Operation 10
/**
 * Fonction permettant de valider tous les groupes d'un projet, si un des groupes est pas complet, tout échoue
 */
CREATE OR REPLACE FUNCTION projet.valider_tous_les_groupes(_id_projet VARCHAR) RETURNS VOID AS $$
DECLARE
    groupe RECORD;
BEGIN
    FOR groupe IN SELECT * FROM projet.groupes g WHERE g.id_projet = _id_projet LOOP
        IF (groupe.complet = false) THEN
            RAISE EXCEPTION 'Le groupe % du projet % n''est pas complet.', groupe.numero_groupe, groupe.id_projet;
        END IF;
    END LOOP;
    UPDATE projet.groupes g SET valide = true WHERE g.id_projet = $1 AND g.complet = true;
END;
$$ LANGUAGE plpgsql;

/*
 * ========================================================================
 * Application étudiante
 * ========================================================================
 */
-- Operation 1
/**
 * Visualiser les cours d'un étudiant
 */
CREATE OR REPLACE VIEW projet.visualiser_cours_etudiant AS
    SELECT DISTINCT et.id_etudiant, co.code_cours, co.nom, COALESCE(string_agg(pr.identifiant_projet,', '),'pas encore de projet') AS "Identifiants des projets" FROM projet.cours co
    LEFT OUTER JOIN projet.projets pr ON co.code_cours = pr.code_cours, projet.etudiants et, projet.inscriptions ic
    WHERE et.id_etudiant = ic.id_etudiant AND co.code_cours = ic.code_cours
    GROUP BY co.code_cours, co.nom, et.id_etudiant;

-- Operation 2
/**
 * Rajouter un étudiant (id_etudiant) à un groupe (id_projet, numero_groupe), échoue si groupe complet ou si l'étudiant est pas inscrit au cours
 */
CREATE OR REPLACE FUNCTION projet.ajouter_etudiant_groupe(_id_etudiant INTEGER, _id_projet VARCHAR, _numero_groupe INTEGER) RETURNS VOID AS $$
BEGIN
    INSERT INTO projet.groupes_etudiants VALUES ($1, $2, $3);
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION projet.ajouter_etudiant_groupe_trigger() RETURNS TRIGGER AS $$
BEGIN
    IF ((SELECT gr.complet FROM projet.groupes gr WHERE gr.numero_groupe = NEW.numero AND gr.id_projet = NEW.id_projet) = true) THEN
        RAISE EXCEPTION 'Le groupe % du projet % est complet.', NEW.numero, NEW.id_projet;
    END IF;
    IF (NOT EXISTS (SELECT * FROM projet.inscriptions i WHERE i.id_etudiant = NEW.id_etudiant AND i.code_cours = (SELECT p.code_cours FROM projet.projets p WHERE p.identifiant_projet = NEW.id_projet))) THEN
        RAISE EXCEPTION 'L''étudiant % n''est pas inscrit au cours du projet %.', NEW.id_etudiant, NEW.id_projet;
    END IF;
    IF ((SELECT gr.nbr_membres_actuel FROM projet.groupes gr WHERE gr.id_projet = NEW.id_projet AND gr.numero_groupe = NEW.numero) + 1 = (SELECT gr1.nbr_places FROM projet.groupes gr1 WHERE gr1.id_projet = NEW.id_projet AND gr1.numero_groupe = NEW.numero)) THEN
        UPDATE projet.groupes SET complet = true WHERE id_projet = NEW.id_projet AND numero_groupe = NEW.numero;
    END IF;
    UPDATE projet.groupes SET nbr_membres_actuel = nbr_membres_actuel+1 WHERE id_projet = NEW.id_projet AND numero_groupe = NEW.numero;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ajouter_etudiant_groupe_trigger
    BEFORE INSERT ON projet.groupes_etudiants
    FOR EACH ROW
    EXECUTE PROCEDURE projet.ajouter_etudiant_groupe_trigger();

-- Operation 3
/**
 * Retirer un étudiant (id_etudiant) d'un groupe (id_projet, numero_groupe), echoue si le groupe est validé ou si l'étudiant n'est pas dans le groupe
 */
CREATE OR REPLACE FUNCTION projet.retirer_etudiant_groupe(_id_etudiant INTEGER, _id_projet VARCHAR) RETURNS VOID AS
$$
DECLARE
    numero_groupe INTEGER := 0;
BEGIN
    IF (NOT EXISTS(SELECT * FROM projet.groupes_etudiants ge WHERE ge.id_etudiant = _id_etudiant AND ge.id_projet = _id_projet)) THEN
        RAISE EXCEPTION 'L''étudiant % n''a pas de groupe dans le projet %.', _id_etudiant, _id_projet;
    END IF;
    SELECT ge.numero FROM projet.groupes_etudiants ge WHERE ge.id_projet = _id_projet AND ge.id_etudiant = _id_etudiant INTO numero_groupe;
    DELETE FROM projet.groupes_etudiants WHERE id_etudiant = $1 AND id_projet = $2 AND numero = numero_groupe;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION projet.retirer_etudiant_groupe_trigger() RETURNS TRIGGER AS $$
BEGIN
    IF ((SELECT gr.valide FROM projet.groupes gr WHERE gr.numero_groupe = OLD.numero AND gr.id_projet = OLD.id_projet) = true) THEN
        RAISE EXCEPTION 'Le groupe % du projet % est validé.', OLD.numero, OLD.id_projet;
    END IF;
    IF ((SELECT gr.complet FROM projet.groupes gr WHERE gr.numero_groupe = OLD.numero AND id_projet = OLD.id_projet) = true) THEN
        UPDATE projet.groupes SET complet = false WHERE id_projet = OLD.id_projet AND numero_groupe = OLD.numero;
    END IF;
    UPDATE projet.groupes SET nbr_membres_actuel = nbr_membres_actuel-1 WHERE id_projet = OLD.id_projet AND numero_groupe = OLD.numero;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER retirer_etudiant_groupe_trigger
    BEFORE DELETE ON projet.groupes_etudiants
    FOR EACH ROW
    EXECUTE PROCEDURE projet.retirer_etudiant_groupe_trigger();

-- Operation 4
/**
 * Visualiser tous les projets (identifiant, nom, code_cours, numero_groupe) des cours ou l"étudiant est inscrit, s'il ne fait pas partie d'un groupe, le numéro de groupe est NULL
 */
CREATE OR REPLACE VIEW projet.visualiser_projets_etudiant AS
    SELECT DISTINCT isc.id_etudiant, pr.identifiant_projet, pr.nom, pr.code_cours, ge.numero
    FROM projet.inscriptions isc
        INNER JOIN projet.projets pr ON isc.code_cours = pr.code_cours
        LEFT OUTER JOIN projet.groupes_etudiants ge on isc.id_etudiant = ge.id_etudiant AND ge.id_projet = pr.identifiant_projet;

-- Operation 5
/**
 * Visualiser les projets (id_projet, nom, code_cours, date_debut, date_fin) ou l'etudiant donné n'a pas de groupe
 */
CREATE OR REPLACE VIEW projet.visualiser_projets_etudiant_sans_groupe AS
    SELECT DISTINCT isc.id_etudiant, pr.identifiant_projet, pr.nom, pr.code_cours, pr.date_debut, pr.date_fin
    FROM projet.inscriptions isc
        INNER JOIN projet.projets pr ON isc.code_cours = pr.code_cours
        LEFT OUTER JOIN projet.groupes_etudiants ge on isc.id_etudiant = ge.id_etudiant AND ge.id_projet = pr.identifiant_projet
    WHERE ge.id_etudiant IS NULL;

-- Operation 6
/**
 * Visualiser les compositions de groupes (numero_groupe, nom, prenom, nbr_places_restantes) incomplets
 */
CREATE OR REPLACE VIEW projet.visualiser_groupes_incomplets AS
    SELECT DISTINCT pr.identifiant_projet , gr.numero_groupe , et.nom , et.prenom , gr.nbr_places - gr.nbr_membres_actuel AS place_disponible
    FROM projet.projets pr
        LEFT OUTER JOIN projet.groupes gr ON pr.identifiant_projet = gr.id_projet
        LEFT OUTER JOIN projet.groupes_etudiants ge ON gr.id_projet = ge.id_projet AND gr.numero_groupe = ge.numero
        LEFT OUTER JOIN projet.inscriptions ic ON  ic.id_etudiant = ge.id_etudiant
        LEFT OUTER JOIN projet.etudiants et ON ic.id_etudiant = et.id_etudiant
    WHERE gr.complet = false
    order BY gr.numero_groupe;

-- Se connecter
/**
 * permet de se connecter a l'application
 */
CREATE OR REPLACE FUNCTION projet.connecter_etudiant(_email VARCHAR) RETURNS RECORD AS $$
    DECLARE
        result RECORD;
BEGIN
    IF EXISTS (SELECT et.id_etudiant FROM projet.etudiants et WHERE et.email = _email ) THEN
        SELECT et.id_etudiant, et.mot_de_passe FROM projet.etudiants et WHERE et.email = _email INTO result;
        RETURN result;
    ELSE
        return 0;
    END IF;
END;
$$ language plpgsql;