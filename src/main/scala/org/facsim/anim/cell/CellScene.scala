/*
Facsimile -- A Discrete-Event Simulation Library
Copyright © 2004-2013, Michael J Allen.

This file is part of Facsimile.

Facsimile is free software: you can redistribute it and/or modify it under the
terms of the GNU Lesser General Public License as published by the Free
Software Foundation, either version 3 of the License, or (at your option) any
later version.

Facsimile is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
details.

You should have received a copy of the GNU Lesser General Public License along
with Facsimile.  If not, see http://www.gnu.org/licenses/lgpl.

The developers welcome all comments, suggestions and offers of assistance.  For
further information, please visit the project home page at:

  http://facsim.org/

Thank you for your interest in the Facsimile project!

IMPORTANT NOTE: All patches (modifications to existing files and/or the
addition of new files) submitted for inclusion as part of the official
Facsimile code base, must comply with the published Facsimile Coding Standards.
If your code fails to comply with the standard, then your patches will be
rejected.  For further information, please visit the coding standards at:

  http://facsim.org/Documentation/CodingStandards/
===============================================================================
Scala source file from the org.facsim.anim.cell package.
*/
//=============================================================================

package org.facsim.anim.cell

import com.sun.j3d.loaders.IncorrectFormatException
import com.sun.j3d.loaders.ParsingErrorException
import com.sun.j3d.loaders.Scene
import java.io.IOException
import java.net.URL
import java.util.Hashtable
import javax.media.j3d.Background
import javax.media.j3d.Behavior
import javax.media.j3d.BranchGroup
import javax.media.j3d.Fog
import javax.media.j3d.Light
import javax.media.j3d.Sound
import javax.media.j3d.TransformGroup
import org.facsim.LibResource
import org.facsim.SafeOption
import org.facsim.io.FieldConversionException
import org.facsim.io.FieldVerificationException
import org.facsim.io.TextReader

//=============================================================================
/**
''Java3D'' scene retrieved from an ''[[http://www.automod.com/ AutoMod®]]
cell'' file.

@constructor Create a new scene from the specified reader and baseUrl.

@param loader Cell loader instance creating this scene.  The loader determines
the flags that determine which nodes in the ''cell'' file&mdash;and in the
files referenced by the ''cell'' file&mdash;are processed during construction
of the scene.

@param reader Text reader that assists with processing the ''cell'' file's
contents.

@param baseUrl Location at which, or relative to which, files referenced within
''cell'' files that have non-absolute paths, will be searched.

@throws [[com.sun.j3d.loaders.IncorrectFormatException!]] if the file supplied
is not an ''AutoMod® cell'' file.

@throws [[com.sun.j3d.loaders.ParsingErrorException!]] if errors are
encountered during parsing of the file.

@since 0.0
*/
//=============================================================================

final class CellScene private [cell] (loader: CellLoader, reader: TextReader,
baseUrl: URL) extends Scene with NotNull {

/*
We should never get a null baseUrl, since we're in control of the information
passed to this class, but let's just make sure.

NOTE: CellLoader and TextReader are both derived from NotNull, so it ought to
be impossible for them to be null.
*/

  assert (baseUrl != null)

/**
Process the cell data.

Each time a new cell is read, this scene is notified so that it can be indexed
and cataloged correctly.

Note: There is a single ''root'' cell element that is either a leaf primitive
or a collection that contains all remaining cells making up the scene.
Consequently, the root cell contains the entire scene itself.
*/

  private final val rootCell = readNextCell (None)

//-----------------------------------------------------------------------------
/**
Read next cell element from the stream.

@param parent Set primitive that is to contain the cell read.  If `None`, then
the cell is the root cell of the scene.

@param isDefinition Flag indicating whether the cell to be read is a definition
cell (`true`) or a regular cell (`false`).  This should be known in advance by
the caller.

@return Cell instance read from the file.  Note that the root cell contains all
cells belonging to this scene as its contents.
*/
//-----------------------------------------------------------------------------

  private [cell] def readNextCell (parent: Option [Set], definitionExpected:
  Boolean = false) = {

/*
Determine the code of the next cell element in the file.
*/

    val cellCode = readInt (CellScene.verifyCellCode (definitionExpected),
    LibResource ("anim.cell.CellScene.readNextCell.cellCodeDesc", if
    (definitionExpected) 1 else 0, CellScene.permittedCellCodes
    (definitionExpected)))

/*
Retrieve the cell class associated with the indicated cell code.
*/

    val cellClass = CellScene.getCellClass (definitionExpected, cellCode)

/*
Determine the constructor for this cell, and invoke it with the appropriate
arguments.

This will fail with an exception if a constructor taking the appropriate
arguments cannot be found.  Needless to say, this shouldn't happen if the
associated class has been supplied with such a constructor.
*/

    val classCtor = cellClass.getConstructor (getClass, classOf [Set])
    classCtor.newInstance (this, parent)
  }

//-----------------------------------------------------------------------------
/**
Helper function to read a verified string value from the stream.

@param verifier Field verification function, used to verify value of field read
before it is returned.

@param description Function that is called to provide a description to supply
to the user in the event that an exception occurs.

@return Value read, if no exceptions arise.

@throws [[com.sun.j3d.loaders.IncorrectFormatException!]] if the file supplied
is not an ''AutoMod® cell'' file.

@throws [[com.sun.j3d.loaders.ParsingErrorException!]] if errors are
encountered during parsing of the file.

@since 0.0
*/
//-----------------------------------------------------------------------------

  private [cell] def readString (verifier: TextReader.Verifier [String],
  description: => String) = {
    val value = try {
      reader.readString (verifier)
    }
    catch {
      case e: Throwable => CellScene.translateReaderException (e, LibResource
      ("anim.cell.CellScene.readValue", description))
    }
    value
  }

//-----------------------------------------------------------------------------
/**
Helper function to read an unrestricted integer value from the stream.

@param description Function that is called to provide a description to supply
to the user in the event that an exception occurs.

@return Value read, if no exceptions arise.

@throws [[com.sun.j3d.loaders.IncorrectFormatException!]] if the file supplied
is not an ''AutoMod® cell'' file.

@throws [[com.sun.j3d.loaders.ParsingErrorException!]] if errors are
encountered during parsing of the file.

@since 0.0
*/
//-----------------------------------------------------------------------------

  private [cell] def readInt (description: => String) = {
    val value = try {
      reader.readInt ()
    }
    catch {
      case e: Throwable => CellScene.translateReaderException (e, LibResource
      ("anim.cell.CellScene.readValue", description))
    }
    value
  }

//-----------------------------------------------------------------------------
/**
Helper function to read a verified integer value from the stream.

@param verifier Field verification function, used to verify value of field read
before it is returned.

@param description Function that is called to provide a description to supply
to the user in the event that an exception occurs.

@return Value read, if no exceptions arise.

@throws [[com.sun.j3d.loaders.IncorrectFormatException!]] if the file supplied
is not an ''AutoMod® cell'' file.

@throws [[com.sun.j3d.loaders.ParsingErrorException!]] if errors are
encountered during parsing of the file.

@since 0.0
*/
//-----------------------------------------------------------------------------

  private [cell] def readInt (verifier: TextReader.Verifier [Int], description:
  => String) = {
    val value = try {
      reader.readInt (verifier)
    }
    catch {
      case e: Throwable => CellScene.translateReaderException (e, LibResource
      ("anim.cell.CellScene.readValue", description))
    }
    value
  }

//-----------------------------------------------------------------------------
/**
Helper function to read an unrestricted double value from the stream.

@param description Function that is called to provide a description to supply
to the user in the event that an exception occurs.

@return Value read, if no exceptions arise.

@throws [[com.sun.j3d.loaders.IncorrectFormatException!]] if the file supplied
is not an ''AutoMod® cell'' file.

@throws [[com.sun.j3d.loaders.ParsingErrorException!]] if errors are
encountered during parsing of the file.

@since 0.0
*/
//-----------------------------------------------------------------------------

  private [cell] def readDouble (description: => String) = {
    val value = try {
      reader.readDouble ()
    }
    catch {
      case e: Throwable => CellScene.translateReaderException (e, LibResource
      ("anim.cell.CellScene.readValue", description))
    }
    value
  }

//-----------------------------------------------------------------------------
/**
Helper function to read a verified double value from the stream.

@param verifier Field verification function, used to verify value of field read
before it is returned.

@param description Function that is called to provide a description to supply
to the user in the event that an exception occurs.

@return Value read, if no exceptions arise.

@throws [[com.sun.j3d.loaders.IncorrectFormatException!]] if the file supplied
is not an ''AutoMod® cell'' file.

@throws [[com.sun.j3d.loaders.ParsingErrorException!]] if errors are
encountered during parsing of the file.

@since 0.0
*/
//-----------------------------------------------------------------------------

  private [cell] def readDouble (verifier: TextReader.Verifier [Double],
  description: => String) = {
    val value = try {
      reader.readDouble (verifier)
    }
    catch {
      case e: Throwable  => CellScene.translateReaderException (e, LibResource
      ("anim.cell.CellScene.readValue", description))
    }
    value
  }

  final override def getSceneGroup(): BranchGroup = ???

  final override def getViewGroups(): Array [TransformGroup] = ???

  final override def getHorizontalFOVs(): Array [Float] = ???

  final override def getLightNodes(): Array [Light] = ???

  final override def getNamedObjects(): Hashtable [String, Object] = ???

  final override def getBackgroundNodes(): Array [Background] = ???

  final override def getFogNodes(): Array [Fog] = ???

  final override def getBehaviorNodes(): Array [Behavior] = ???

  final override def getSoundNodes(): Array [Sound] = ???

  final override def getDescription(): String = ???

// Get TCFs
  final def getTerminals(): Array [BranchGroup] = ???

// Get Joints
  final def getJoints(): Array [BranchGroup] = ???
}

//=============================================================================
/**
CellScene companion object.

@since 0.0
*/
//=============================================================================

private [cell] object CellScene {

/**
Type representing class information for a sub-class of
[[org.facsim.anim.cell.Cell!]].
*/

  type CellClass = Class [_ <: Cell]

/**
Map linking definition state (true = definition cell, false = regular cell) to
a map linking cell code to cell class.
*/

  private [this] val partitionedClassMap = partitionClassMap

//-----------------------------------------------------------------------------
/**
Function to initialize the relation between definition/regular cell status and
maps of cell code to cell class.

Regular cell elements (sets, tetrahedra, vector lists, instances, etc.) can
appear in the normal tree of elements.  However, definition cell elements can
only appear at the root of a definition&mdash;they are then included in the
scene via reference (by an instance cell element).

Because of the different places in which these two different types of cell are
defined, it makes sense to utilize two different ''cell code to cell class''
maps, one for regular cell elements and the other for definition cell elements.

This function starts with a map of cell codes to cell classes, and then
partitions it into two maps, one for definition cells and the other for regular
cells.

@return Map relating cell definition state (`true` = definition cell, `false` =
regular cell) to a map that relates cell codes to cell classes.

@see [[http://facsim.org/Documentation/Resources/AutoModCellFile/Type.html
AutoMod Cell Type Codes]]

@since 0.0
*/
//-----------------------------------------------------------------------------

  private [this] def partitionClassMap = {

/*
Map associating cell type code with corresponding ''regular'' cell class.

All classes contained in his map must be concrete classes and must provide a
constructor taking CellScene reference and an Option [Set] arguments.
 
@note This map is defined in order of the cell codes for ease of maintenance by
a human (the resulting map itself is not ordered by cell code).  Please
maintain this order when modifying the list.
*/

    val classMap = Map [Int, CellClass] (
      100 -> classOf [Triad],
      115 -> classOf [VectorList],
      125 -> classOf [Polyhedron],
      130 -> classOf [Arc],                   // Originally, coarse arc
      131 -> classOf [Arc],                   // Originally, fine arc
      140 -> classOf [WorldText],
      141 -> classOf [ScreenFastText],
      142 -> classOf [ScreenNormalText],
      143 -> classOf [UnrotateFastText],
      144 -> classOf [UnrotateNormalText],
      150 -> classOf [WorldTextList],
      151 -> classOf [ScreenFastTextList],
      152 -> classOf [ScreenNormalTextList],
      153 -> classOf [UnrotateFastTextList],
      154 -> classOf [UnrotateNormalTextList],
      308 -> classOf [BlockDefinition],
      310 -> classOf [Trapezoid],
      311 -> classOf [Tetrahedron],
      315 -> classOf [Rectangle],
      330 -> classOf [Hemisphere],            // Originally, coarse hemisphere
      331 -> classOf [Hemisphere],            // Originally, fine hemisphere
      340 -> classOf [Cone],                  // Originally, coarse cone
      341 -> classOf [Cone],                  // Originally, fine cone
      350 -> classOf [Cylinder],              // Originally, coarse cylinder
      351 -> classOf [Cylinder],              // Originally, fine cylinder
      360 -> classOf [Frustum],               // Originally, coarse frustum
      361 -> classOf [Frustum],               // Originally, fine frustum
      388 -> classOf [FileReference],
      408 -> classOf [Instance],
      555 -> classOf [CompiledPicture],
      599 -> classOf [EmbeddedFile],
      700 -> classOf [RegularSet],
      7000 -> classOf [RegularSet],
      10000 -> classOf [RegularSet]
  )

/*
Class of the Definition cell type.

All sub-classes of the Definition class are regarded as special cases: they
can only be defined as a definition, and not as part of the primary cell scene.
*/

    val definition = classOf [Definition]

/*
Helper function to determine if a class is a definition sub-class or not.
*/

    def isDefinition (cellClass: CellClass) =
    definition.isAssignableFrom (cellClass)

/*
Partition the map into two: one containing regular cell classes, and the other
containing definition cell classes.
*/

    val (definitionClassMap, regularClassMap) = classMap.partition (p =>
    isDefinition (p._2))

/*
Now construct, and return, the map relating definition state to class map.
*/

    Map [Boolean, Map [Int, CellClass]] (
      true -> definitionClassMap,
      false -> regularClassMap
    )
  }

//-----------------------------------------------------------------------------
/**
Function to verify a cell code.

@param definitionExpected If `true` the cell code read must be for a definition
cell; if `false`, the cell code must be for a regular cell.

@param cellCode Cell code read from the data file.

@return `true` if the cell code read from the file is value; `false` if it is
not a valid expected cell code.

@see [[http://facsim.org/Documentation/Resources/AutoModCellFile/Type.html
AutoMod Cell Type Codes]]

@since 0.0 
*/
//-----------------------------------------------------------------------------

  private def verifyCellCode (definitionExpected: Boolean)(cellCode: Int) =
  partitionedClassMap (definitionExpected).contains (cellCode)

//-----------------------------------------------------------------------------
/**
Function to report the set of permitted cell codes.

The set of codes permitted depends upon whether we're expecting a definition
(`definitionExpected` == `true`) or regular (`definitionExpected` == `false`)
cell code.

@param definitionExpected If `true` the cell code read must be for a definition
cell; if `false`, the cell code must be for a regular cell.

@return String containing a list of permitted cell codes.

@see [[http://facsim.org/Documentation/Resources/AutoModCellFile/Type.html
AutoMod Cell Type Codes]]

@since 0.0
*/
//-----------------------------------------------------------------------------

  private def permittedCellCodes (definitionExpected: Boolean) =
  partitionedClassMap (definitionExpected).keys.toList.sorted.mkString (", ")

//-----------------------------------------------------------------------------
/**
Function to lookup the associated cell class for the specified cell code.

@param definitionExpected Flag indicating whether we're expecting a definition
cell (`true`) or a regular cell (`false`).

@param cellCode Integer code for which a cell class is to be looked-up.

@return Class information for the specified cell code.

@see [[http://facsim.org/Documentation/Resources/AutoModCellFile/Type.html
AutoMod Cell Type Codes]]

@since 0.0
*/
//-----------------------------------------------------------------------------

  private def getCellClass (definitionExpected: Boolean, cellCode: Int) =
  partitionedClassMap (definitionExpected)(cellCode)

//-----------------------------------------------------------------------------
/**
Translate a reader exception.

This function translates caught exceptions from [[org.facsim.io.TextReader!]]
operations into more appropriate cell error exceptions.  In addition, it adds
some extra information to the exception to assist with debugging cell read
failures.

@note The distinction between a [[com.sun.j3d.loaders.ParsingErrorException!]]
(indicating that a file of the wrong type was passed to a loader) and a
[[com.sun.j3d.loaders.IncorrectFormatException!]] (indicating that a problem
parsing the file was encountered) can be a fine one.  This function attempts to
address this distinction in a standard manner.

Firstly, all [[org.facsim.io.FieldConversionException]]s, indicating that cell
data failed to match the expected type of data (string, integer, double, etc.),
are treated as `IncorrectFormatException`s.  If a string is encountered when an
integer is expected, for instance, then that's a pretty good sign that the file
may not be a valid cell file.

[[org.facsim.io.FieldVerificationException]]s are more troublesome.  Mapping
all such exceptions to `IncorrectFormatException`s may be too severe, while
mapping them to `ParsingErrorException`s may be too lenient.  Still, a standard
mapping is required, so ''Facsimile'' treats such exceptions as the latter
type.

Similarly, IOExceptions are also treated as `ParsingErrorException`s.

It's possible that these rules may be refined in future editions to improve
handling of 3D file importing.

@param exception Exception to be translated.

@param msg A message to be added to the exception to explain what might have
just happened.

@return This function does not return and always throws an exception.
*/
//-----------------------------------------------------------------------------

  private [cell] def translateReaderException (exception: Throwable, msg:
  String): Nothing = exception match {

/*
Map field conversion exceptions (expected an Int, but got a string, for
example) to incorrect format exceptions.
*/

    case e: FieldConversionException =>
    throw new IncorrectFormatException (msg).initCause (e)

/*
Map field verification exceptions (data is correct type, but doesn't have an
acceptable value, such as an integer being outside of an allowed range) to
parsing error exceptions.
*/

    case e: FieldVerificationException =>
    throw new ParsingErrorException (msg).initCause (e)

/*
Map I/O exceptions (of which there are many different types) to parsing error
exceptions.
*/

    case e: IOException => throw new ParsingErrorException (msg).initCause (e)

/*
For all other exceptions, re-throw the original exception.
*/

    case _ => throw exception
  }
}