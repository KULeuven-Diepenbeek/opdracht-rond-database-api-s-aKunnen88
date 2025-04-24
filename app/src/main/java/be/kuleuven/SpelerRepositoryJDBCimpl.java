package be.kuleuven;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SpelerRepositoryJDBCimpl implements SpelerRepository {
  private Connection connection;

  // Constructor
  SpelerRepositoryJDBCimpl(Connection connection) {
    this.connection = connection;
  }

  @Override
  public void addSpelerToDb(Speler speler) {
      try{ 
        PreparedStatement prepared = (PreparedStatement) connection
          .prepareStatement("INSERT INTO speler (tennisvlaanderenid, naam, punten) VALUES (?, ?, ?);");
        prepared.setInt(1, speler.getTennisvlaanderenid());
        prepared.setString(2, speler.getNaam());
        prepared.setInt(3, speler.getPunten());
        prepared.executeUpdate();

        prepared.close();
        connection.commit();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
  }
  


  @Override
  public Speler getSpelerByTennisvlaanderenId(int tennisvlaanderenId) {
    Speler speler = null;
    try{
      Statement s = (Statement) connection.createStatement();
      String statement = "SELECT * FROM speler WHERE tennisvlaanderenid = '" + tennisvlaanderenId + "'";
      ResultSet result = s.executeQuery(statement);

      while(result.next()){
        int id = result.getInt("tennisvlaanderenid");
        String naam = result.getString("naam");
        int punten = result.getInt("punten");
        
        speler = new Speler(id, naam, punten);
       }
       if (speler == null) {
        throw new InvalidSpelerException(tennisvlaanderenId + "");
      }
      result.close();
      s.close();
      connection.commit();

    } catch(Exception e){
      throw new RuntimeException(e);
    }
    return speler;
  }

  @Override
  public List<Speler> getAllSpelers() {
    ArrayList<Speler> resultList = new ArrayList<Speler>();
    try {
      Statement s = (Statement) connection.createStatement();
      String stmt = "SELECT * FROM speler";
      ResultSet result = s.executeQuery(stmt);

      while (result.next()) {
        int tennisvlaanderenId = result.getInt("tennisvlaanderenid");
        String naam = result.getString("naam");
        int punten = result.getInt("punten");

        resultList.add(new Speler(tennisvlaanderenId, naam, punten));
      }

      result.close();
      s.close();
      connection.commit();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return resultList;
  }


  @Override
  public void updateSpelerInDb(Speler speler) {
    getSpelerByTennisvlaanderenId(speler.getTennisvlaanderenid());
    String sql = "UPDATE speler SET naam = ?, punten = ? WHERE tennisvlaanderenid = ?";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, speler.getNaam());
      ps.setInt(2, speler.getPunten());
      ps.setInt(3, speler.getTennisvlaanderenid());
      ps.executeUpdate();

      ps.close();
      connection.commit();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void deleteSpelerInDb(int tennisvlaanderenId) {
    getSpelerByTennisvlaanderenId(tennisvlaanderenId);
    String sql = "DELETE FROM speler WHERE tennisvlaanderenid = ?";
    
    try(PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, tennisvlaanderenId);
      ps.executeUpdate();

      ps.close();
      connection.commit();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getHoogsteRankingVanSpeler(int tennisvlaanderenid) {
    getSpelerByTennisvlaanderenId(tennisvlaanderenid);
    String resultString = null;

    try {
        PreparedStatement prepared = (PreparedStatement) connection
          .prepareStatement("SELECT t.clubnaam, w.finale, w.winnaar " +
                            "FROM wedstrijd w " +
                            "JOIN tornooi t ON w.tornooi = t.id " +
                            "WHERE (w.speler1 = ? OR w.speler2 = ?) " +
                            "ORDER BY w.finale ASC " +
                            "LIMIT 1");
        prepared.setInt(1, tennisvlaanderenid);
        prepared.setInt(2, tennisvlaanderenid);
        ResultSet result = prepared.executeQuery();

        if (result.next()) {
          String clubnaam = result.getString("clubnaam");
          int finale = result.getInt("finale");
          Integer winnaar = result.getObject("winnaar") != null ? result.getInt("winnaar") : null;

          String finaleString;
          if (finale == 1 && winnaar != null && winnaar == tennisvlaanderenid) {
             finaleString = "winst";
          } else if (finale == 1) {
            finaleString = "finale";
          } else if (finale == 2) {
            finaleString = "halve-finale";
          } else if (finale == 4) {
            finaleString = "kwart-finale";
          } else {
            finaleString = "lager dan de kwart-finales";
          }

          resultString = "Hoogst geplaatst in het tornooi van " + clubnaam + " met plaats in de " + finaleString;
        } else {
          resultString = "Geen resultaten van deze speler";
        }

        result.close();
        prepared.close();
        connection.commit();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return resultString;
  }


  @Override
  public void addSpelerToTornooi(int tornooiId, int tennisvlaanderenId){
    try {
      PreparedStatement prepared = (PreparedStatement) connection
        .prepareStatement("INSERT INTO speler_speelt_tornooi (tornooi, speler) VALUES (?, ?);");
      prepared.setInt(1, tornooiId);
      prepared.setInt(2, tennisvlaanderenId);
      prepared.executeUpdate();

      prepared.close();
      connection.commit();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void removeSpelerFromTornooi(int tornooiId, int tennisvlaanderenId) {
    try {
        PreparedStatement prepared = (PreparedStatement) connection.prepareStatement("DELETE FROM speler_speelt_tornooi WHERE tornooi = ? AND speler = ?;");
        prepared.setInt(1, tornooiId);
        prepared.setInt(2, tennisvlaanderenId);
        prepared.executeUpdate();

        prepared.close();
        connection.commit();
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
  }
}
