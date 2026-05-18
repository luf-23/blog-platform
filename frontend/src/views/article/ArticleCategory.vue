<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus } from "@element-plus/icons-vue";

import PageHeader from "../../components/common/PageHeader.vue";
import EmptyState from "../../components/common/EmptyState.vue";
import CategoryCard from "../../components/article/CategoryCard.vue";
import {
  getCategoryListService,
  addCategoryService,
  updateCategoryService,
  deleteCategoryService
} from "../../api/category.js";

const router = useRouter();

const categories = ref([]);
const loading = ref(false);

async function fetchCategories() {
  loading.value = true;
  try {
    const result = await getCategoryListService();
    categories.value = (result.data || []).map((item) => ({
      id: item.categoryId,
      name: item.categoryName,
      description: item.categoryDescription || "",
      createTime: item.createTime,
      updateTime: item.updateTime
    }));
  } finally {
    loading.value = false;
  }
}

fetchCategories();

const dialogVisible = ref(false);
const dialogMode = ref("add");
const dialogTitle = ref("添加分类");
const formRef = ref(null);
const formData = reactive({
  categoryId: "",
  categoryName: "",
  categoryDescription: ""
});

const originalName = ref("");

const rules = {
  categoryName: [
    { required: true, message: "分类名不能为空", trigger: "blur" },
    { min: 1, max: 10, message: "长度在 1-10 个字符", trigger: "blur" },
    {
      validator: (_, value, callback) => {
        if (dialogMode.value === "edit" && value === originalName.value) {
          return callback();
        }
        const exists = categories.value.some((item) => item.name === value);
        return exists ? callback(new Error("该分类名已存在")) : callback();
      },
      trigger: "blur"
    }
  ],
  categoryDescription: [
    { max: 30, message: "长度不能超过 30 字符", trigger: "blur" }
  ]
};

function openAdd() {
  dialogMode.value = "add";
  dialogTitle.value = "添加分类";
  Object.assign(formData, {
    categoryId: "",
    categoryName: "",
    categoryDescription: ""
  });
  originalName.value = "";
  dialogVisible.value = true;
}

function openEdit(category) {
  dialogMode.value = "edit";
  dialogTitle.value = "编辑分类";
  Object.assign(formData, {
    categoryId: category.id,
    categoryName: category.name,
    categoryDescription: category.description
  });
  originalName.value = category.name;
  dialogVisible.value = true;
}

async function submit() {
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  try {
    if (dialogMode.value === "add") {
      await addCategoryService({
        categoryName: formData.categoryName,
        categoryDescription: formData.categoryDescription
      });
      ElMessage.success("添加成功");
    } else {
      await updateCategoryService(formData);
      ElMessage.success("更新成功");
    }
    dialogVisible.value = false;
    fetchCategories();
  } catch (error) {
    console.error(error);
  }
}

function remove(category) {
  ElMessageBox.confirm(
    `确定要删除「${category.name}」吗？删除后该分类下的文章也将无法访问。`,
    "删除分类",
    {
      confirmButtonText: "删除",
      cancelButtonText: "取消",
      type: "warning"
    }
  )
    .then(async () => {
      await deleteCategoryService({ categoryId: category.id });
      ElMessage.success("删除成功");
      categories.value = categories.value.filter((c) => c.id !== category.id);
    })
    .catch(() => {});
}

function goToList(category) {
  router.push({
    name: "ArticleList",
    query: { categoryId: category.id }
  });
}
</script>

<template>
  <div class="bp-page">
    <PageHeader title="我的分类" subtitle="点击分类卡片可查看分类下的文章列表">
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openAdd">
          新建分类
        </el-button>
      </template>
    </PageHeader>

    <div v-loading="loading">
      <div v-if="categories.length" class="grid">
        <CategoryCard
          v-for="category in categories"
          :key="category.id"
          :category="category"
          @click="goToList"
          @edit="openEdit"
          @delete="remove"
        />
      </div>
      <EmptyState
        v-else-if="!loading"
        title="还没有任何分类"
        description="先创建一个分类，再开始你的写作之旅"
      >
        <el-button type="primary" :icon="Plus" @click="openAdd">
          创建第一个分类
        </el-button>
      </EmptyState>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="480px"
      align-center
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-position="top"
      >
        <el-form-item label="分类名" prop="categoryName">
          <el-input
            v-model="formData.categoryName"
            placeholder="给分类起个名字"
          />
        </el-form-item>
        <el-form-item label="描述" prop="categoryDescription">
          <el-input
            v-model="formData.categoryDescription"
            type="textarea"
            :rows="3"
            placeholder="简单介绍一下这个分类（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">
          {{ dialogMode === "add" ? "创建" : "保存" }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
</style>
