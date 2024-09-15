use anyhow::{anyhow, Result};

pub trait IntoResult<T> {
    fn res(self) -> Result<T>;
}

impl<T> IntoResult<T> for Option<T> {
    fn res(self) -> Result<T> {
        self.ok_or(anyhow!("Tried to unwrap a `None` value!"))
    }
}
