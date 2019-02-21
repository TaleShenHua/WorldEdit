/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.forge;

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.IntegerProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.stream.Collectors;

final class ForgeAdapter {

    private ForgeAdapter() {
    }

    public static World adapt(net.minecraft.world.World world) {
        return new ForgeWorld(world);
    }

    public static Vector3 adapt(Vec3d vector) {
        return Vector3.at(vector.x, vector.y, vector.z);
    }

    public static BlockVector3 adapt(BlockPos pos) {
        return BlockVector3.at(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Vec3d toVec3(BlockVector3 vector) {
        return new Vec3d(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    public static EnumFacing adapt(Direction face) {
        switch (face) {
            case NORTH: return EnumFacing.NORTH;
            case SOUTH: return EnumFacing.SOUTH;
            case WEST: return EnumFacing.WEST;
            case EAST: return EnumFacing.EAST;
            case DOWN: return EnumFacing.DOWN;
            case UP:
            default:
                return EnumFacing.UP;
        }
    }

    public static Direction adaptEnumFacing(EnumFacing face) {
        switch (face) {
            case NORTH: return Direction.NORTH;
            case SOUTH: return Direction.SOUTH;
            case WEST: return Direction.WEST;
            case EAST: return Direction.EAST;
            case DOWN: return Direction.DOWN;
            case UP:
            default:
                return Direction.UP;
        }
    }

    public static BlockPos toBlockPos(BlockVector3 vector) {
        return new BlockPos(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    public static Property<?> adaptProperty(IProperty<?> property) {
        if (property instanceof PropertyBool) {
            return new BooleanProperty(property.getName(), ImmutableList.copyOf(((PropertyBool) property).getAllowedValues()));
        }
        if (property instanceof PropertyInteger) {
            return new IntegerProperty(property.getName(), ImmutableList.copyOf(((PropertyInteger) property).getAllowedValues()));
        }
        if (property instanceof PropertyDirection) {
            return new DirectionalProperty(property.getName(), ((PropertyDirection) property).getAllowedValues().stream()
                    .map(ForgeAdapter::adaptEnumFacing)
                    .collect(Collectors.toList()));
        }
        if (property instanceof PropertyEnum) {
            return new EnumProperty(property.getName(), ((PropertyEnum<?>) property).getAllowedValues().stream()
                    .map(e -> e.getName())
                    .collect(Collectors.toList()));
        }
        return new IPropertyAdapter<>(property);
    }
    
    public static IBlockState adaptState(BlockStateHolder<?> block) {
        Block mcBlock = Block.getBlockFromName(block.getBlockType().getId());
        IBlockState newState = mcBlock.getDefaultState();
        Map<Property<?>, Object> states = block.getStates();
        newState = applyProperties(mcBlock.getBlockState(), newState, states);
        return newState;
    }

    // Can't get the "Object" to be right for withProperty w/o this
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static IBlockState applyProperties(BlockStateContainer stateContainer, IBlockState newState, Map<Property<?>, Object> states) {
        for (Map.Entry<Property<?>, Object> state : states.entrySet()) {

            IProperty property = stateContainer.getProperty(state.getKey().getName());
            Comparable value = (Comparable) state.getValue();
            // we may need to adapt this value, depending on the source prop
            if (property instanceof PropertyDirection) {
                Direction dir = (Direction) value;
                value = ForgeAdapter.adapt(dir);
            } else if (property instanceof PropertyEnum) {
                String enumName = (String) value;
                value = ((PropertyEnum<?>) property).parseValue((String) value).or(() -> {
                    throw new IllegalStateException("Enum property " + property.getName() + " does not contain " + enumName);
                });
            }

            newState = newState.withProperty(property, value);
        }
        return newState;
    }

}
