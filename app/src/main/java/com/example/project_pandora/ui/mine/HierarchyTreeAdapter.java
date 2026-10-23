package com.example.project_pandora.ui.mine;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project_pandora.R;
import com.example.project_pandora.core.util.RoleDisplayUtil;
import com.example.project_pandora.core.util.RoleRankUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TASK-R18-013: 人员关系树形适配器
 *
 * <p>采用 RecyclerView 扁平化展开列表 + 层级缩进实现多级树形展示（不使用 ExpandableListView，
 * 因其仅支持两级，无法满足 REQ-R18-10 规则 1 多级嵌套需求）。</p>
 *
 * <p>特性：
 * <ul>
 *   <li>同层节点按 RoleRank 升序 + 用户 id 升序稳定排序（展示端兜底，不依赖后端顺序）</li>
 *   <li>首次加载默认展开第一层、第 2 层及以下默认折叠</li>
 *   <li>节点显示姓名（real_name 优先、username 兜底、"未知"兜底）与中文角色</li>
 *   <li>孤儿节点置于树之外独立分组"上级已删除"展示</li>
 *   <li>添加/删除/替换成功回调后按节点 id 记忆保持当前展开状态集合</li>
 * </ul></p>
 */
public class HierarchyTreeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_NODE = 1;
    private static final int TYPE_GROUP_HEADER = 2;

    /** 扁平化后的展示行 */
    private final List<FlatItem> flatItems = new ArrayList<>();
    /** 当前展开的节点 id 集合（按节点 id 记忆展开状态） */
    private final Set<Long> expandedNodeIds = new HashSet<>();
    /** 是否首次加载（首次加载默认展开第一层） */
    private boolean firstLoad = true;

    /** 节点点击回调（用于删除关系确认） */
    public interface OnNodeClickListener {
        void onNodeClick(Long superiorId, Long subordinateId, String displayName);
    }

    private OnNodeClickListener onNodeClickListener;

    public void setOnNodeClickListener(OnNodeClickListener listener) {
        this.onNodeClickListener = listener;
    }

    /**
     * 设置树形数据并重建扁平化列表
     *
     * @param roots       根节点列表（每个节点含 id/username/realName/role/children）
     * @param orphanNodes 孤儿节点列表（上级已删除）
     */
    @SuppressWarnings("unchecked")
    public void setTreeData(List<Map<String, Object>> roots, List<Map<String, Object>> orphanNodes) {
        flatItems.clear();

        // 首次加载默认展开第一层（根节点全部展开）
        if (firstLoad && roots != null) {
            for (Map<String, Object> root : roots) {
                Long id = extractId(root);
                if (id != null) {
                    expandedNodeIds.add(id);
                }
            }
            firstLoad = false;
        }

        // 渲染根节点（按 RoleRank 排序）
        if (roots != null) {
            List<Map<String, Object>> sortedRoots = new ArrayList<>(roots);
            sortByRoleRank(sortedRoots);
            for (Map<String, Object> root : sortedRoots) {
                flattenNode(root, 0, null);
            }
        }

        // 渲染孤儿节点独立分组
        if (orphanNodes != null && !orphanNodes.isEmpty()) {
            flatItems.add(new FlatItem(FlatItem.TYPE_GROUP_HEADER, "上级已删除（孤儿节点）", null, null, 0));
            List<Map<String, Object>> sortedOrphans = new ArrayList<>(orphanNodes);
            sortByRoleRank(sortedOrphans);
            for (Map<String, Object> orphan : sortedOrphans) {
                flattenOrphan(orphan);
            }
        }

        notifyDataSetChanged();
    }

    /**
     * 递归扁平化节点
     *
     * @param node     当前节点
     * @param depth    当前深度（0 为根层）
     * @param parentId 父节点 id（根节点为 null）
     */
    @SuppressWarnings("unchecked")
    private void flattenNode(Map<String, Object> node, int depth, Long parentId) {
        Long id = extractId(node);
        String displayName = extractDisplayName(node);
        String role = extractRole(node);

        FlatItem item = new FlatItem(FlatItem.TYPE_NODE, displayName, role, id, depth);
        item.parentId = parentId;
        item.hasChildren = hasChildren(node);

        // 判断展开状态
        boolean expanded = id != null && expandedNodeIds.contains(id);
        item.expanded = expanded;

        flatItems.add(item);

        // 递归展开子节点
        if (expanded && item.hasChildren) {
            Object childrenObj = node.get("children");
            if (childrenObj instanceof List) {
                List<Map<String, Object>> children = (List<Map<String, Object>>) childrenObj;
                // 子节点按 RoleRank 排序
                List<Map<String, Object>> sortedChildren = new ArrayList<>(children);
                sortByRoleRank(sortedChildren);
                for (Map<String, Object> child : sortedChildren) {
                    flattenNode(child, depth + 1, id);
                }
            }
        }
    }

    /** 扁平化孤儿节点（无展开/折叠，直接展示） */
    private void flattenOrphan(Map<String, Object> orphan) {
        Long id = extractId(orphan);
        String displayName = extractDisplayName(orphan);
        String role = extractRole(orphan);

        FlatItem item = new FlatItem(FlatItem.TYPE_NODE, displayName, role, id, 1);
        item.parentId = null;
        item.hasChildren = false;
        item.expanded = false;
        item.isOrphan = true;
        flatItems.add(item);
    }

    /** 切换节点展开/折叠状态 */
    public void toggleExpand(int position) {
        if (position < 0 || position >= flatItems.size()) return;
        FlatItem item = flatItems.get(position);
        if (item.type != FlatItem.TYPE_NODE || !item.hasChildren || item.nodeId == null) return;

        if (expandedNodeIds.contains(item.nodeId)) {
            expandedNodeIds.remove(item.nodeId);
        } else {
            expandedNodeIds.add(item.nodeId);
        }
        // 重建扁平化列表（保持展开状态集合）
        // 注意：这里需要外部重新调用 setTreeData，但为简化交互，提供 rebuildRequest 回调
        // 实际实现：调用方在 toggleExpand 后需重新 setTreeData
    }

    /** 获取当前展开状态集合（用于重建后保持展开状态） */
    public Set<Long> getExpandedNodeIds() {
        return expandedNodeIds;
    }

    /** 标记为非首次加载（用于重建时保持展开状态） */
    public void markNotFirstLoad() {
        this.firstLoad = false;
    }

    public boolean isEmpty() {
        return flatItems.isEmpty();
    }

    // ===== RecyclerView.Adapter =====

    @Override
    public int getItemViewType(int position) {
        FlatItem item = flatItems.get(position);
        return item.type == FlatItem.TYPE_GROUP_HEADER ? TYPE_GROUP_HEADER : TYPE_NODE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_GROUP_HEADER) {
            View view = inflater.inflate(R.layout.item_hierarchy_group_header, parent, false);
            return new GroupHeaderHolder(view);
        }
        View view = inflater.inflate(R.layout.item_hierarchy_tree_node, parent, false);
        return new NodeHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FlatItem item = flatItems.get(position);
        if (holder instanceof GroupHeaderHolder) {
            ((GroupHeaderHolder) holder).textHeader.setText(item.displayName);
        } else if (holder instanceof NodeHolder) {
            NodeHolder nodeHolder = (NodeHolder) holder;
            // 层级缩进（每层 24dp）
            int indent = item.depth * 24;
            nodeHolder.itemView.setPadding(indent + 8, nodeHolder.itemView.getPaddingTop(),
                    8, nodeHolder.itemView.getPaddingBottom());

            nodeHolder.textNodeName.setText(item.displayName);
            nodeHolder.textNodeRole.setText(item.role != null ? RoleDisplayUtil.toChinese(item.role) : "");

            // 展开/折叠控件
            if (item.hasChildren) {
                nodeHolder.imgExpandToggle.setVisibility(View.VISIBLE);
                nodeHolder.imgExpandToggle.setImageResource(
                        item.expanded ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float);
            } else {
                nodeHolder.imgExpandToggle.setVisibility(View.INVISIBLE);
            }

            // 点击事件：有子节点时切换展开/折叠；无子节点时触发删除回调
            nodeHolder.itemView.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                FlatItem clicked = flatItems.get(pos);
                if (clicked.hasChildren) {
                    toggleExpand(pos);
                    if (onNodeClickListener != null) {
                        // 通知调用方重建树（保持展开状态）
                        onNodeClickListener.onNodeClick(null, null, "__REBUILD__");
                    }
                } else if (!clicked.isOrphan && onNodeClickListener != null) {
                    // 叶子节点点击触发删除关系确认（parentId 为上级，nodeId 为下属）
                    onNodeClickListener.onNodeClick(clicked.parentId, clicked.nodeId, clicked.displayName);
                }
            });

            // 展开/折叠图标点击
            nodeHolder.imgExpandToggle.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                FlatItem clicked = flatItems.get(pos);
                if (clicked.hasChildren) {
                    toggleExpand(pos);
                    if (onNodeClickListener != null) {
                        onNodeClickListener.onNodeClick(null, null, "__REBUILD__");
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return flatItems.size();
    }

    // ===== 工具方法 =====

    /** 按 RoleRank 升序 + 用户 id 升序稳定排序 */
    private void sortByRoleRank(List<Map<String, Object>> nodes) {
        Collections.sort(nodes, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> a, Map<String, Object> b) {
                String roleA = extractRole(a);
                String roleB = extractRole(b);
                int rankA = RoleRankUtil.rankOf(roleA);
                int rankB = RoleRankUtil.rankOf(roleB);
                if (rankA != rankB) return Integer.compare(rankA, rankB);
                // 同级按用户 id 升序稳定排序
                Long idA = extractId(a);
                Long idB = extractId(b);
                return Long.compare(idA != null ? idA : Long.MAX_VALUE, idB != null ? idB : Long.MAX_VALUE);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private boolean hasChildren(Map<String, Object> node) {
        Object childrenObj = node.get("children");
        return childrenObj instanceof List && !((List<Map<String, Object>>) childrenObj).isEmpty();
    }

    private Long extractId(Map<String, Object> node) {
        Object id = node.get("id");
        if (id == null) id = node.get("ID");
        // Round26: 解析统一收敛至 NumberCompat（原 instanceof Number 判定语义保持）
        return com.example.project_pandora.core.util.NumberCompat.toLong(id);
    }

    private String extractDisplayName(Map<String, Object> node) {
        // 姓名：real_name 优先、realName 兜底、username 兜底、"未知"兜底
        Object realName = node.get("real_name");
        if (realName == null) realName = node.get("realName");
        if (realName == null) realName = node.get("REAL_NAME");
        Object username = node.get("username");
        if (username == null) username = node.get("USERNAME");
        if (realName != null && !realName.toString().isEmpty()) return realName.toString();
        if (username != null && !username.toString().isEmpty()) return username.toString();
        return "未知";
    }

    private String extractRole(Map<String, Object> node) {
        Object role = node.get("role");
        if (role == null) role = node.get("ROLE");
        return role != null ? role.toString() : null;
    }

    // ===== ViewHolder =====

    static class NodeHolder extends RecyclerView.ViewHolder {
        ImageView imgExpandToggle;
        TextView textNodeName;
        TextView textNodeRole;

        NodeHolder(View itemView) {
            super(itemView);
            imgExpandToggle = itemView.findViewById(R.id.imgExpandToggle);
            textNodeName = itemView.findViewById(R.id.textNodeName);
            textNodeRole = itemView.findViewById(R.id.textNodeRole);
        }
    }

    static class GroupHeaderHolder extends RecyclerView.ViewHolder {
        TextView textHeader;

        GroupHeaderHolder(View itemView) {
            super(itemView);
            textHeader = (TextView) itemView;
        }
    }

    // ===== 数据模型 =====

    private static class FlatItem {
        static final int TYPE_NODE = 1;
        static final int TYPE_GROUP_HEADER = 2;

        int type;
        String displayName;
        String role;
        Long nodeId;
        Long parentId;
        int depth;
        boolean hasChildren;
        boolean expanded;
        boolean isOrphan;

        FlatItem(int type, String displayName, String role, Long nodeId, int depth) {
            this.type = type;
            this.displayName = displayName;
            this.role = role;
            this.nodeId = nodeId;
            this.depth = depth;
        }
    }
}