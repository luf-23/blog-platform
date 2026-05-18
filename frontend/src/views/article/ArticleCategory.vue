<script setup>
import { reactive, ref, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import CategoryCard from "../../components/CategoryCard.vue";
import {
  getCategoryListService,
  addCategoryService,
  updateCategoryService,
  deleteCategoryService
} from "../../api/category.js";
import { ElMessage, ElMessageBox } from "element-plus";
const router = useRouter();
const tableData = reactive([]);

// 添加窗口宽度响应式变量
const dialogWidth = ref(window.innerWidth > 768 ? "40%" : "90%");

// 添加窗口大小变化监听函数
const updateDialogWidth = () => {
  dialogWidth.value = window.innerWidth > 768 ? "40%" : "90%";
};

// 在组件挂载时添加监听器
onMounted(() => {
  window.addEventListener("resize", updateDialogWidth);
});

// 在组件卸载时移除监听器
onUnmounted(() => {
  window.removeEventListener("resize", updateDialogWidth);
});

const getArticleCategoryList = async () => {
  const result = await getCategoryListService();
  Object.assign(
    tableData,
    result.data.map((item) => {
      return {
        id: item.categoryId,
        name: item.categoryName,
        description: item.categoryDescription ? item.categoryDescription : "无",
        createTime: item.createTime,
        updateTime: item.updateTime
      };
    })
  );
};
getArticleCategoryList();

// 新增分类
const dialogForAdd = ref(false);
const formDataForAdd = reactive({
  categoryName: "",
  categoryDescription: ""
});
const rulesForAdd = {
  categoryName: [
    { required: true, message: "分类名不能为空", trigger: "blur" },
    { min: 1, max: 10, message: "长度在 1 到 10 个字符", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        const isExist = tableData.some((item) => item.name === value);
        if (isExist) {
          callback(new Error("该分类名称已存在"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ],
  categoryDescription: [
    { max: 30, message: "长度小于 30 个字符", trigger: "blur" }
  ]
};
const handleAdd = () => {
  dialogForAdd.value = true;
};
const closeAddDialog = () => {
  dialogForAdd.value = false;
  formDataForAdd.categoryName = "";
  formDataForAdd.categoryDescription = "";
};
const formRefForAdd = ref(null);
const submitForAddForm = async () => {
  if (!formRefForAdd.value) return;
  try {
    await formRefForAdd.value.validate();
    // 提交表单数据
    await addCategoryService(formDataForAdd);
    ElMessage.success("添加成功");
    await getArticleCategoryList();
    // 关闭对话框
    closeAddDialog();
  } catch (error) {
    console.error("表单验证失败:", error);
  }
};

// 编辑分类
const dialogForEdit = ref(false);
const originalCategoryName = ref(""); // 添加存储原始分类名
const formDataForEdit = reactive({
  categoryId: "",
  categoryName: "",
  categoryDescription: ""
});

const rulesForEdit = {
  categoryName: [
    { required: true, message: "分类名不能为空", trigger: "blur" },
    { min: 1, max: 10, message: "长度在 1 到 10 个字符", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        // 如果名称没有改变，直接通过验证
        if (value === originalCategoryName.value) {
          callback();
          return;
        }
        // 检查是否与其他分类名重复
        const isExist = tableData.some((item) => item.name === value);
        if (isExist) {
          callback(new Error("该分类名称已存在"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ],
  categoryDescription: [
    { max: 30, message: "长度小于 30 个字符", trigger: "blur" }
  ]
};

const handleEdit = (row) => {
  dialogForEdit.value = true;
  originalCategoryName.value = row.name; // 保存原始分类名
  formDataForEdit.categoryId = row.id;
  formDataForEdit.categoryName = row.name;
  formDataForEdit.categoryDescription = row.description;
};

const closeEditDialog = () => {
  dialogForEdit.value = false;
  formDataForEdit.categoryName = "";
  formDataForEdit.categoryDescription = "";
  originalCategoryName.value = ""; // 清除原始分类名
  formDataForEdit.categoryId = "";
};

const formRefForEdit = ref(null);

const submitForEditForm = async () => {
  if (!formRefForEdit.value) return;
  try {
    await formRefForEdit.value.validate();
    // 提交表单数据
    await updateCategoryService(formDataForEdit);
    ElMessage.success("更新成功");
    await getArticleCategoryList();
    // 关闭对话框
    closeEditDialog();
  } catch (error) {
    console.error("表单验证失败:", error);
  }
};

// 删除分类
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除分类"${row.name}"吗？`, "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  })
    .then(async () => {
      try {
        await deleteCategoryService({
          categoryId: row.id
        });
        ElMessage.success("删除成功");
        // 直接从tableData中移除被删除的项
        const index = tableData.findIndex((item) => item.id === row.id);
        if (index > -1) {
          tableData.splice(index, 1);
        }
        //await getArticleCategoryList();
      } catch (error) {
        console.error("删除失败:", error);
        ElMessage.error("删除失败，请重试");
      }
    })
    .catch(() => {
      ElMessage.info("已取消删除");
    });
};

// 点击行事件
const handleRowClick = (rowData) => {
  // 可以在这里执行其他操作，比如跳转到详情页等
  router.push({
    name: "ArticleList",
    query: {
      categoryId: rowData.id
    }
  });
};
</script>

<template>
  <div class="category-container">
    <div class="category-header">
      <div class="header-content">
        <h2 class="page-title">文章分类</h2>
        <p class="page-subtitle">
          点击分类卡片可查看该分类下的文章列表，或在该分类下写新文章
        </p>
      </div>
      <el-button type="primary" @click="handleAdd">添加分类</el-button>
    </div>

    <template v-if="tableData.length !== 0">
      <div class="category-grid">
        <CategoryCard
          v-for="item in tableData"
          :key="item.id"
          :category="{
            name: item.name,
            description: item.description,
            createTime: item.createTime,
            updateTime: item.updateTime
          }"
          @click="handleRowClick(item)"
          @edit="handleEdit(item)"
          @delete="handleDelete(item)"
        />
      </div>
    </template>
    <el-empty
      v-else
      description="暂无文章分类,点击右上角创建"
      :image-size="200"
    />

    <!-- 添加分类对话框 -->
    <el-dialog
      v-model="dialogForAdd"
      title="添加文章分类"
      :width="dialogWidth"
      :modal="true"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRefForAdd"
        :model="formDataForAdd"
        :rules="rulesForAdd"
        label-width="80px"
      >
        <el-form-item label="分类名" prop="categoryName">
          <el-input v-model="formDataForAdd.categoryName"></el-input>
        </el-form-item>
        <el-form-item label="描述" prop="categoryDescription">
          <el-input
            v-model="formDataForAdd.categoryDescription"
            type="textarea"
            :rows="4"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeAddDialog">取消</el-button>
        <el-button type="primary" @click="submitForAddForm">提交</el-button>
      </template>
    </el-dialog>

    <!-- 编辑分类对话框 -->
    <el-dialog
      v-model="dialogForEdit"
      title="编辑"
      :width="dialogWidth"
      :modal="true"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRefForEdit"
        :model="formDataForEdit"
        :rules="rulesForEdit"
        label-width="80px"
      >
        <el-form-item label="分类名" prop="categoryName">
          <el-input v-model="formDataForEdit.categoryName"></el-input>
        </el-form-item>
        <el-form-item label="描述" prop="categoryDescription">
          <el-input
            v-model="formDataForEdit.categoryDescription"
            type="textarea"
            :rows="4"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeEditDialog">取消</el-button>
        <el-button type="primary" @click="submitForEditForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.category-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  padding: 16px 24px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.header-content {
  flex: 1;
}

.page-title {
  margin: 0 0 8px 0;
  color: #2c3e50;
  font-size: 24px;
  font-weight: 600;
}

.page-subtitle {
  margin: 0;
  color: #6c757d;
  font-size: 14px;
  line-height: 1.5;
}

.page-title::after {
  content: "";
  position: absolute;
  bottom: -8px;
  left: 0;
  width: 50px;
  height: 4px;
  background: linear-gradient(135deg, #3a8ee6 0%, #0056b3 100%);
  border-radius: 4px;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  padding: 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 3px 15px rgba(0, 0, 0, 0.08);
}

:deep(.el-dialog) {
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

:deep(.el-dialog__header) {
  padding: 20px 24px;
  border-bottom: 1px solid #ebeef5;
  margin-right: 0;
}

:deep(.el-dialog__body) {
  padding: 24px;
}

:deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #ebeef5;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #2c3e50;
}

:deep(.el-input__wrapper) {
  border-radius: 6px;
  padding: 0 16px;
}

:deep(.el-input__inner) {
  height: 32px;
}

:deep(.el-textarea__inner) {
  padding: 8px 16px;
  border-radius: 6px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.view-toggle {
  margin-left: 16px;
}

@media (max-width: 768px) {
  .category-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    padding: 12px 16px;
  }

  .header-content {
    text-align: left;
  }

  .page-title {
    font-size: 20px;
    margin-bottom: 6px;
  }

  .page-subtitle {
    font-size: 13px;
  }

  .category-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  :deep(.el-dialog) {
    width: 90% !important;
    margin: 0 auto;
  }
}
</style>
