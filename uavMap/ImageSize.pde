
public class ImageSize {
  private float with;
  private float height;
  
  public float getWith() {
    return with;
  }
  public void setWith(float with) {
    this.with = with;
  }
  public float getHeight() {
    return height;
  }
  public void setHeight(float height) {
    this.height = height;
  }
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Float.floatToIntBits(height);
    result = prime * result + Float.floatToIntBits(with);
    return result;
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ImageSize other = (ImageSize) obj;
    if (Float.floatToIntBits(height) != Float.floatToIntBits(other.height))
      return false;
    if (Float.floatToIntBits(with) != Float.floatToIntBits(other.with))
      return false;
    return true;
  }
  
}

