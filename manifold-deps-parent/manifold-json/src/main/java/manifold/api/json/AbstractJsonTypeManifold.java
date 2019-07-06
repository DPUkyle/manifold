/*
 * Copyright (c) 2019 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.api.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.script.Bindings;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.type.FragmentValue;
import manifold.api.type.JavaTypeManifold;
import manifold.ext.DataBindings;
import manifold.ext.RuntimeMethods;
import manifold.ext.api.IProxyFactory;
import manifold.ext.api.Structural;
import manifold.util.JsonUtil;
import manifold.util.ManClassUtil;

/**
 * A base class for a Type Manifold modeled with a {@link JsonModel} e.g., parses its resource to a {@link Bindings}.
 * For instance, both the JSON and YAML manifolds extend this class because they both parse to a common {@code Bindings}
 * based model.
 */
public abstract class AbstractJsonTypeManifold<T extends JsonModel> extends JavaTypeManifold<T>
{
  private static final Set<String> API_INNER_CLASS_NAMES =
    new HashSet<>( Arrays.asList( "Proxy", "ProxyFactory", "Builder", "Copier" ) );

  @Override
  public boolean isInnerType( String topLevel, String relativeInner )
  {
    JsonModel model = getModel( topLevel );
    IJsonParentType type = model == null ? null : model.getType();
    if( type == null )
    {
      return false;
    }

    if( isApiInnerClass( topLevel, relativeInner ) )
    {
      return true;
    }

    IJsonParentType csr = type;
    StringBuilder enclosing = new StringBuilder( topLevel );
    for( StringTokenizer tokenizer = new StringTokenizer( relativeInner, "." ); tokenizer.hasMoreTokens(); )
    {
      String childName = tokenizer.nextToken();
      IJsonType child = csr.findChild( childName );
      if( child instanceof IJsonParentType )
      {
        csr = (IJsonParentType)child;
        enclosing.append( '.' ).append( childName );
        continue;
      }
      else if( child == null )
      {
        return isApiInnerClass( enclosing.toString(), childName );
      }
      return false;
    }
    return true;
  }

  /**
   * These inner classes are generated as part of the API, as opposed to being defined in JSON.
   *
   * @param topLevel The name of the enclosing class of {@code relativeInner}
   * @param relativeInner The name of an API inner class to test for
   * @return true if {@code relativeInner} is an inner class generated by the API
   */
  @SuppressWarnings({"WeakerAccess", "unused"})
  protected boolean isApiInnerClass( String topLevel, String relativeInner )
  {
    return API_INNER_CLASS_NAMES.contains( relativeInner );
  }

  @Override
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, String existing, T model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    StringBuilder sb = new StringBuilder();
    String pkg = ManClassUtil.getPackage( topLevelFqn );
    sb.append( "package " ).append( pkg ).append( ";\n\n" )
      .append( "import " ).append( Json.class.getName() ).append( ";\n" )
      .append( "import " ).append( JsonUtil.class.getName() ).append( ";\n" )
      .append( "import " ).append( Bindings.class.getName() ).append( ";\n" )
      .append( "import " ).append( Loader.class.getName() ).append( ";\n" )
      .append( "import " ).append( Requester.class.getName() ).append( ";\n" )
      .append( "import " ).append( List.class.getName() ).append( ";\n" )
      .append( "import " ).append( ArrayList.class.getName() ).append( ";\n" )
      .append( "import " ).append( Map.class.getName() ).append( ";\n" )
      .append( "import " ).append( DataBindings.class.getName() ).append( ";\n" )
      .append( "import " ).append( IJsonBindingsBacked.class.getName() ).append( ";\n" )
      .append( "import " ).append( IJsonList.class.getName() ).append( ";\n" )
      .append( "import " ).append( Structural.class.getName() ).append( ";\n" )
      .append( "import " ).append( FragmentValue.class.getName() ).append( ";\n" )
      .append( "import " ).append( IProxyFactory.class.getName() ).append( ";\n" )
      .append( "import " ).append( RuntimeMethods.class.getName() ).append( ";\n\n" );
    model.report( errorHandler );
    model.getType().render( this, sb, 0, true );
    return sb.toString();
  }
}