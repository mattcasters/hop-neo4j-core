package org.neo4j.hop.model;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.apache.hop.core.gui.plugin.GuiMetaStoreElement;
import org.apache.hop.core.gui.plugin.GuiPlugin;
import org.apache.hop.metastore.IHopMetaStoreElement;
import org.apache.hop.metastore.api.IMetaStore;
import org.apache.hop.metastore.persist.MetaStoreFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.metastore.persist.MetaStoreAttribute;
import org.apache.hop.metastore.persist.MetaStoreElementType;
import org.neo4j.hop.shared.NeoConnection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@MetaStoreElementType(
  name = "Neo4j Graph Model",
  description = "Description of the nodes, relationships, indexes... of a Neo4j graph"
)
public class GraphModel implements IHopMetaStoreElement<GraphModel> {
  protected String name;

  @MetaStoreAttribute
  protected String description;

  @MetaStoreAttribute
  protected List<GraphNode> nodes;

  @MetaStoreAttribute
  protected List<GraphRelationship> relationships;

  public GraphModel() {
    nodes = new ArrayList<>();
    relationships = new ArrayList<>();
  }

  public GraphModel( String name, String description, List<GraphNode> nodes, List<GraphRelationship> relationships ) {
    this.name = name;
    this.description = description;
    this.nodes = nodes;
    this.relationships = relationships;
  }

  /**
   * Create a graph model from a JSON string
   * @param jsonModelString the model in JSON string format
   */
  public GraphModel(String jsonModelString) throws HopException {
    this();

    try {
      JSONParser parser = new JSONParser();
      JSONObject jsonModel = (JSONObject) parser.parse( jsonModelString );

      setName( (String) jsonModel.get("name") );
      setDescription( (String) jsonModel.get("description") );

      // Parse all the nodes...
      //
      JSONArray jsonNodes = (JSONArray) jsonModel.get("nodes");
      if (jsonNodes!=null) {
        for ( Object jsonNode1 : jsonNodes ) {
          JSONObject jsonNode = (JSONObject) jsonNode1;
          GraphNode graphNode = new GraphNode();
          graphNode.setName( (String) jsonNode.get( "name" ) );
          graphNode.setDescription( (String) jsonNode.get( "description" ) );

          // Parse node labels
          //
          JSONArray jsonLabels = (JSONArray) jsonNode.get( "labels" );
          if ( jsonLabels != null ) {
            for ( String jsonLabel : (Iterable<String>) jsonLabels ) {
              graphNode.getLabels().add( jsonLabel );
            }
          }

          // Parse node properties
          //
          JSONArray jsonProperties = (JSONArray) jsonNode.get( "properties" );
          if ( jsonProperties != null ) {
            for ( JSONObject jsonProperty : (Iterable<JSONObject>) jsonProperties ) {
              graphNode.getProperties().add( parseGraphPropertyJson( jsonProperty ) );
            }
          }

          JSONObject jsonPresentation = (JSONObject) jsonNode.get( "presentation" );
          if ( jsonPresentation != null ) {
            long x = (Long)jsonPresentation.get("x");
            long y = (Long)jsonPresentation.get("y");
            graphNode.setPresentation( new GraphPresentation( (int)x, (int)y ) );
          }

          nodes.add( graphNode );
        }
      }

      // Parse the relationships...
      //
      JSONArray jsonRelationships = (JSONArray) jsonModel.get("relationships");
      if (jsonRelationships!=null) {
        for ( Object jsonRelationship1 : jsonRelationships ) {
          JSONObject jsonRelationship = (JSONObject) jsonRelationship1;
          GraphRelationship graphRelationship = new GraphRelationship();
          graphRelationship.setName( (String) jsonRelationship.get( "name" ) );
          graphRelationship.setDescription( (String) jsonRelationship.get( "description" ) );
          graphRelationship.setLabel( (String) jsonRelationship.get( "label" ) );
          graphRelationship.setNodeSource( (String) jsonRelationship.get( "source" ) );
          graphRelationship.setNodeTarget( (String) jsonRelationship.get( "target" ) );

          // Parse relationship properties
          //
          JSONArray jsonProperties = (JSONArray) jsonRelationship.get( "properties" );
          if (jsonProperties!=null) {
            for ( JSONObject jsonProperty : (Iterable<JSONObject>) jsonProperties ) {
              graphRelationship.getProperties().add( parseGraphPropertyJson( jsonProperty ) );
            }
          }
          relationships.add( graphRelationship );
        }
      }
    } catch ( Exception e ) {
      throw new HopException( "Error serializing to JSON", e );
    }
  }

  private GraphProperty parseGraphPropertyJson( JSONObject jsonProperty ) {
    GraphProperty graphProperty = new GraphProperty(  );
    graphProperty.setName( (String) jsonProperty.get("name") );
    graphProperty.setDescription( (String) jsonProperty.get("description") );
    graphProperty.setPrimary( (boolean) jsonProperty.get("primary") );
    graphProperty.setType( GraphPropertyType.parseCode( (String) jsonProperty.get("type") ) );
    return graphProperty;
  }

  public String getJSONString() throws HopException {

    try {
      JSONObject jsonModel = new JSONObject();
      jsonModel.put( "name", name );
      if ( StringUtils.isNotEmpty(description)) {
        jsonModel.put( "description", description );
      }

      // Add all the node information to the JSON model
      //
      JSONArray jsonNodes = new JSONArray();
      for ( GraphNode graphNode : nodes ) {
        JSONObject jsonNode = new JSONObject();
        jsonNode.put( "name", graphNode.getName() );
        if ( StringUtils.isNotEmpty(graphNode.getDescription())) {
          jsonNode.put( "description", graphNode.getDescription() );
        }

        // Add the labels
        //
        JSONArray jsonLabels = new JSONArray();
        for ( String label : graphNode.getLabels() ) {
          jsonLabels.add( label );
        }
        jsonNode.put( "labels", jsonLabels );

        // Add the properties...
        //
        JSONArray jsonProperties = new JSONArray();
        for ( GraphProperty graphProperty : graphNode.getProperties() ) {
          jsonProperties.add( getJsonProperty( graphProperty ) );
        }
        jsonNode.put( "properties", jsonProperties );

        // The presentation...
        //
        GraphPresentation presentation = graphNode.getPresentation();
        JSONObject jsonPresentation = new JSONObject();
        jsonPresentation.put("x", presentation.getX());
        jsonPresentation.put("y", presentation.getY());
        jsonNode.put("presentation", jsonPresentation);

        // Add the json encoded node object to the list
        //
        jsonNodes.add(jsonNode);
      }
      jsonModel.put( "nodes", jsonNodes );

      // Add the relationships to the JSON model
      //
      JSONArray jsonRelationships = new JSONArray();
      for ( GraphRelationship graphRelationship : relationships ) {
        JSONObject jsonRelationship = new JSONObject();
        jsonRelationship.put( "name", graphRelationship.getName() );
        if (StringUtils.isNotEmpty( graphRelationship.getDescription() )) {
          jsonRelationship.put( "description", graphRelationship.getDescription() );
        }
        jsonRelationship.put( "label", graphRelationship.getLabel() );
        jsonRelationship.put( "source", graphRelationship.getNodeSource() );
        jsonRelationship.put( "target", graphRelationship.getNodeTarget() );

        // Save the properties as well
        //
        JSONArray jsonProperties = new JSONArray();
        for ( GraphProperty graphProperty : graphRelationship.getProperties() ) {
          jsonProperties.add( getJsonProperty( graphProperty ) );
        }
        jsonRelationship.put( "properties", jsonProperties );

        jsonRelationships.add( jsonRelationship );
      }
      jsonModel.put( "relationships", jsonRelationships );

      String jsonString = jsonModel.toJSONString();

      // Pretty print JSON
      //
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      JsonParser jp = new JsonParser();
      JsonElement je = jp.parse(jsonString);

      return gson.toJson(je);
    } catch(Exception e) {
      throw new HopException( "Error encoding model in JSON", e );
    }
  }

  private JSONObject getJsonProperty( GraphProperty graphProperty ) {
    JSONObject jsonProperty = new JSONObject();
    jsonProperty.put( "name", graphProperty.getName() );
    if (StringUtils.isNotEmpty( graphProperty.getDescription() )) {
      jsonProperty.put( "description", graphProperty.getDescription() );
    }
    jsonProperty.put( "type", GraphPropertyType.getCode( graphProperty.getType() ) );
    jsonProperty.put( "primary", graphProperty.isPrimary() );
    return jsonProperty;
  }

  @Override public boolean equals( Object object ) {
    if ( object == null ) {
      return false;
    }
    if ( !( object instanceof GraphModel ) ) {
      return false;
    }
    if ( object == this ) {
      return true;
    }
    return ( (GraphModel) object ).getName().equalsIgnoreCase( name );
  }

  public GraphModel(GraphModel source) {
    this();
    replace(source);
  }

  public void replace( GraphModel source ) {
    setName( source.getName() );
    setDescription( source.getDescription() );

    // Copy nodes
    //
    nodes = new ArrayList<>();
    for ( GraphNode node : source.getNodes() ) {
      nodes.add( new GraphNode(node) );
    }

    // replace relationships
    //
    relationships = new ArrayList<>();
    for ( GraphRelationship relationship : source.getRelationships() ) {
      relationships.add( new GraphRelationship(relationship) );
    }
  }


  /**
   * Find a node with the given name, matches case insensitive
   *
   * @param nodeName
   * @return The mode with the given name or null if the node was not found
   */
  public GraphNode findNode( String nodeName ) {
    for ( GraphNode node : nodes ) {
      if ( node.getName().equalsIgnoreCase( nodeName ) ) {
        return node;
      }
    }
    return null;
  }

  /**
   * @return a sorted list of node names
   */
  public String[] getNodeNames() {
    String[] names = new String[ nodes.size() ];
    for ( int i = 0; i < names.length; i++ ) {
      names[ i ] = nodes.get( i ).getName();
    }
    Arrays.sort( names );
    return names;
  }


  /**
   * Find a relationship with the given name, matches case insensitive
   *
   * @param relationshipName
   * @return The relationship with the given name or null if the relationship was not found
   */
  public GraphRelationship findRelationship( String relationshipName ) {
    for ( GraphRelationship relationship : relationships ) {
      if ( relationship.getName().equalsIgnoreCase( relationshipName ) ) {
        return relationship;
      }
    }
    return null;
  }

  /**
   * @return a sorted list of relationship names
   */
  public String[] getRelationshipNames() {
    String[] names = new String[ relationships.size() ];
    for ( int i = 0; i < names.length; i++ ) {
      names[ i ] = relationships.get( i ).getName();
    }
    Arrays.sort( names );
    return names;
  }


  public String getName() {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public List<GraphNode> getNodes() {
    return nodes;
  }

  public void setNodes( List<GraphNode> nodes ) {
    this.nodes = nodes;
  }

  public List<GraphRelationship> getRelationships() {
    return relationships;
  }

  public void setRelationships( List<GraphRelationship> relationships ) {
    this.relationships = relationships;
  }

  /**
   * Find a relationship with source and target, case insensitive
   *
   * @param source
   * @param target
   * @return the relationship or null if nothing was found.
   */
  public GraphRelationship findRelationship( String source, String target ) {
    for ( GraphRelationship relationship : relationships ) {
      if ( relationship.getNodeSource().equalsIgnoreCase( source ) &&
        relationship.getNodeTarget().equalsIgnoreCase( target ) ) {
        return relationship;
      }
    }
    return null;
  }

  @Override public MetaStoreFactory<GraphModel> getFactory( IMetaStore metaStore ) {
    return createFactory( metaStore );
  }

  public static final MetaStoreFactory<GraphModel> createFactory( IMetaStore metaStore ) {
    return new MetaStoreFactory<>( GraphModel.class, metaStore );
  }
}
