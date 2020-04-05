package org.neo4j.hop.model;

import org.apache.hop.metastore.persist.MetaStoreAttribute;

import java.util.Objects;

public class GraphProperty {

  @MetaStoreAttribute
  protected String name;

  @MetaStoreAttribute
  protected String description;

  @MetaStoreAttribute
  protected GraphPropertyType type;

  @MetaStoreAttribute
  protected boolean primary;

  public GraphProperty() {
  }

  public GraphProperty( String name, String description, GraphPropertyType type, boolean primary ) {
    this.name = name;
    this.description = description;
    this.type = type;
    this.primary = primary;
  }

  @Override public boolean equals( Object o ) {
    if ( this == o ) {
      return true;
    }
    if ( o == null || getClass() != o.getClass() ) {
      return false;
    }
    GraphProperty that = (GraphProperty) o;
    return Objects.equals( name, that.name );
  }

  @Override public int hashCode() {
    return Objects.hash( name );
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

  public GraphPropertyType getType() {
    return type;
  }

  public void setType( GraphPropertyType type ) {
    this.type = type;
  }

  public boolean isPrimary() {
    return primary;
  }

  public void setPrimary( boolean primary ) {
    this.primary = primary;
  }
}