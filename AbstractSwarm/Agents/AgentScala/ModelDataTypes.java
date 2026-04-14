/*
AbstractSwarm agent interface for Scala
Copyright (C) 2021  Lars Hadidi (lahadidi@uni-mainz.de)
                    Daan Apeldoorn (daan.apeldoorn@uni-mainz.de)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/


import java.util.List;
import java.util.Map;

class ModelDataTypes {}

class AbstractSwarmObject { }

class ComponentType extends AbstractSwarmObject
{
    public String name;
    public String type;

    public int frequency;
    public int necessity;
    public int time;
    public int cycle;

    public List<VisitEdge> visitEdges;
    public List<TimeEdge> timeEdges;
    public List<PlaceEdge> placeEdges;
}

class StationType extends ComponentType
{
    public List<Station> components;

    public int space;
}

class AgentType extends ComponentType
{
    public List<Agent> components;

    public int size;
    public int priority;
}

class Edge extends AbstractSwarmObject
{
    public ComponentType connectedType;
    public String type;
}

class VisitEdge extends Edge
{
    public boolean bold;
}

class WeightedEdge extends Edge
{
    public boolean incoming;
    public boolean outgoing;
    public int weight;
}

class PlaceEdge extends WeightedEdge { }

class TimeEdge extends WeightedEdge
{
    public boolean andOrigin;
    public boolean andConnected;
}

class Component extends AbstractSwarmObject
{
    public String name;

    public int frequency;
    public Map<TimeEdge, Integer> cycles;
}

class Station extends Component
{
    public StationType type;

    public int space;
    public Map<Agent, Integer> necessities;
}

class Agent extends Component
{
    public AgentType type;

    public int time;
    public Map<Station, Integer> necessities;
    public Station target;
    public Station previousTarget;
    public boolean visiting;
}